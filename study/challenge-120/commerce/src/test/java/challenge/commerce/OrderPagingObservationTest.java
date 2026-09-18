package challenge.commerce;

import static org.junit.jupiter.api.Assertions.*;

import challenge.commerce.infra.db.OrderPagingFixture;
import jakarta.persistence.EntityManagerFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import javax.sql.DataSource;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

/** H2 로컬 관찰 전용. 성공은 B009 풀이 통과를 의미하지 않는다. */
class OrderPagingObservationTest extends CommerceHttpSupport {
    @Autowired
    DataSource source;

    @Autowired
    EntityManagerFactory emf;

    private final StringBuilder report = new StringBuilder();

    private void note(String format, Object... args) {
        report.append(String.format(format, args)).append("\n");
    }

    @AfterEach
    void cleanLargeFixture() {
        new OrderPagingFixture(source).clear();
    }

    @Test
    void observeExistingListAndDatabaseWork() throws Exception {
        assertFalse(MYSQL, "H2 전용 도구. -Pmysql을 함께 사용하지 않는다.");
        var data = new OrderPagingFixture(source);
        var jdbc = data.jdbc();
        data.seed(6);
        var before = pageIds(0);
        data.appendLatest();
        var after = pageIds(1);
        assertTrue(after.stream().anyMatch(before::contains));
        note("[사건] 첫 3개=%s, 새 주문 삽입 후 OFFSET 3=%s (중복 존재)", before, after);
        data.clear();
        var rows = data.seed(20_000);
        assertEquals(20_000, jdbc.queryForObject("select count(*) from store_order", Integer.class));
        assertEquals(40_000, jdbc.queryForObject("select count(*) from store_order_item", Integer.class));
        note(
                "H2=%s, 주문=20000, 품목=40000, 결제=%d",
                jdbc.queryForObject("select h2version()", String.class),
                jdbc.queryForObject("select count(*) from store_payment", Integer.class));
        var stats = emf.unwrap(SessionFactory.class).getStatistics();
        boolean enabled = stats.isStatisticsEnabled();
        stats.setStatisticsEnabled(true);
        try {
            stats.clear();
            long start = System.nanoTime();
            var response = raw("GET", "/api/orders", "");
            assertEquals(200, response.statusCode());
            double ms = (System.nanoTime() - start) / 1_000_000.0;
            assertEquals(20, json.readTree(response.body()).path("entries").size());
            assertTrue(stats.getEntityLoadCount() <= 80, "2만 건 중 현재 페이지 관련 엔티티만 적재");
            note(
                    "[기본 페이지 HTTP size=20] SQL=%d, entityLoad=%d, UTF8 bytes=%d, wall=%.3f ms (단회·직렬화/네트워크 포함)",
                    stats.getPrepareStatementCount(),
                    stats.getEntityLoadCount(),
                    response.body().getBytes(StandardCharsets.UTF_8).length,
                    ms);
            if (Boolean.getBoolean("b009.solution")) {
                for (int size : new int[] {20, 100}) {
                    stats.clear();
                    start = System.nanoTime();
                    var page = raw("GET", "/api/orders/window?size=" + size, "");
                    assertEquals(200, page.statusCode(), "B009 구현 후에만 -Pb009Solution 사용");
                    assertEquals(
                            size, json.readTree(page.body()).path("entries").size());
                    note(
                            "[사용자 HTTP size=%d] SQL=%d, entityLoad=%d, bytes=%d, wall=%.3f ms",
                            size,
                            stats.getPrepareStatementCount(),
                            stats.getEntityLoadCount(),
                            page.body().getBytes(StandardCharsets.UTF_8).length,
                            (System.nanoTime() - start) / 1_000_000.0);
                }
            } else note("[사용자 HTTP] 미측정. 구현 후 -Pb009Solution으로 추가한다.");
        } finally {
            stats.setStatisticsEnabled(enabled);
        }
        var boundary = rows.get(17_999);
        note("[깊은 위치 경계] 이미 18000개 읽은 마지막 주문: id=%d, createdAt=%s", boundary.id(), boundary.time());
        String candidate = new ClassPathResource("b009-query.sql")
                .getContentAsString(StandardCharsets.UTF_8)
                .replaceAll("(?m)--[^\r\n]*", "")
                .trim()
                .replace("${boundaryId}", Long.toString(boundary.id()))
                .replace("${boundaryTime}", boundary.time().toString());
        try {
            for (String phase : new String[] {"기존 인덱스", "선택한 인덱스 적용 후"}) {
                if (phase.equals("선택한 인덱스 적용 후")) {
                    String ddl = new ClassPathResource("b009-index.sql").getContentAsString(StandardCharsets.UTF_8);
                    if (ddl.replaceAll("(?m)--[^\r\n]*", "").isBlank()) {
                        note("[인덱스 미선택] b009-index.sql이 비어 있어 두 번째 측정 생략");
                        break;
                    }
                    new ResourceDatabasePopulator(new ClassPathResource("b009-index.sql")).execute(source);
                }
                jdbc.execute("ANALYZE");
                for (int offset : new int[] {0, 18_000})
                    probe(
                            data,
                            phase + " OFFSET=" + offset,
                            "select id,created_at from store_order order by created_at desc,id desc limit 20 offset "
                                    + offset);
                if (!candidate.isBlank()) {
                    int i = 0;
                    for (String query : candidate.split(";"))
                        if (!query.isBlank()) probe(data, phase + " 사용자 SQL " + (++i), query.trim());
                } else note("[사용자 SQL] 미측정. b009-query.sql에 선택한 SELECT를 작성한다.");
            }
        } finally {
            jdbc.execute("drop index if exists b009_candidate");
        }
        note("JDBC 시간: 워밍업3회 후7회 중앙값·결과 읽기 포함·결과 캐시 비활성. scanCount는 H2 관찰값이며 디스크 I/O·MySQL 비용이 아니다.");
        var output = Path.of("build/b009-observation.txt");
        Files.createDirectories(output.getParent());
        Files.writeString(output, report);
        System.out.println(report);
        System.out.println("관찰 기록: " + output.toAbsolutePath());
    }

    private java.util.List<Long> pageIds(int page) throws Exception {
        var response = request("GET", "/api/orders?page=" + page + "&size=3", "");
        assertEquals(200, response.code());
        var ids = new java.util.ArrayList<Long>();
        for (var entry : response.body().path("entries"))
            ids.add(entry.path("order").path("id").asLong());
        return ids;
    }

    private void probe(OrderPagingFixture data, String label, String query) {
        var jdbc = data.jdbc();
        jdbc.execute("SET OPTIMIZE_REUSE_RESULTS FALSE");
        for (int i = 0; i < 3; i++) jdbc.queryForList(query);
        long[] samples = new long[7];
        int count = 0;
        for (int i = 0; i < 7; i++) {
            long start = System.nanoTime();
            count = jdbc.queryForList(query).size();
            samples[i] = System.nanoTime() - start;
        }
        Arrays.sort(samples);
        note(
                "[%s] returned=%d, median=%.3f ms\nSQL: %s\n%s",
                label,
                count,
                samples[3] / 1_000_000.0,
                query,
                jdbc.queryForObject("EXPLAIN ANALYZE " + query, String.class));
    }
}
