package challenge.commerce.infra.db;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;

/** 테스트 DB의 학습용 데이터. 실행 중인 개발 서버에는 연결하지 않는다. */
public class OrderPagingFixture {
    public static final Instant BASE = Instant.parse("2026-09-01T00:00:00Z");
    private final JdbcTemplate jdbc;

    public OrderPagingFixture(DataSource source) {
        jdbc = new JdbcTemplate(source);
    }

    public JdbcTemplate jdbc() {
        return jdbc;
    }

    public record Row(long id, Instant time) {}

    // 같은 시각, ID 순서와 시각 순서가 다른 주문을 함께 만든다.
    public List<Row> seed(int count) {
        var rows = IntStream.range(0, count)
                .mapToObj(i -> new Row(10_000L + i, BASE.plusSeconds(((i * 37L) % count) / 3)))
                .toList();
        jdbc.batchUpdate(
                "insert into store_order(id,total_amount,created_at) values(?,3000,?)", rows, 1000, (ps, row) -> {
                    ps.setLong(1, row.id());
                    ps.setTimestamp(2, Timestamp.from(row.time()));
                });
        for (int position = 0; position < 2; position++) {
            int pos = position;
            jdbc.batchUpdate("""
                insert into store_order_item(order_id,item_position,option_id,product_name,option_name,image,unit_price,quantity,total_amount)
                values(?,?,?,'B009 fixture','White / M','/images/tee.svg',?,1,?)
                """, rows, 1000, (ps, row) -> {
                ps.setLong(1, row.id());
                ps.setInt(2, pos);
                ps.setLong(3, 101 + pos);
                ps.setLong(4, (pos + 1) * 1000);
                ps.setLong(5, (pos + 1) * 1000);
            });
        }
        jdbc.batchUpdate("""
            insert into store_payment(order_id,payment_key,amount,status,approval_id) values(?,?,3000,?,?)
            """, rows.stream().filter(row -> row.id() % 3 != 0).toList(), 1000, (ps, row) -> {
            ps.setLong(1, row.id());
            ps.setString(2, "b009-" + row.id());
            boolean paid = row.id() % 3 == 2;
            ps.setString(3, paid ? "PAID" : "PENDING");
            ps.setString(4, paid ? "approval-" + row.id() : null);
        });
        return rows.stream()
                .sorted(Comparator.comparing(Row::time)
                        .thenComparingLong(Row::id)
                        .reversed())
                .toList();
    }

    public void appendLatest() {
        jdbc.update(
                "insert into store_order(id,total_amount,created_at) values(900000,1000,?)",
                Timestamp.from(BASE.plusSeconds(1_000_000)));
        jdbc.update("""
            insert into store_order_item(order_id,item_position,option_id,product_name,option_name,image,unit_price,quantity,total_amount)
            values(900000,0,101,'NEW','White / M','/images/tee.svg',1000,1,1000)
            """);
    }

    public void clear() {
        jdbc.update("delete from store_payment");
        jdbc.update("delete from store_order_item");
        jdbc.update("delete from store_order");
    }
}
