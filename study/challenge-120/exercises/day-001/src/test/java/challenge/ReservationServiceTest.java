package challenge;

import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import static challenge.ReservationResult.Status.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = ChallengeApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Timeout(30)
class ReservationServiceTest {
    private static final String URL = databaseUrl();
    private static final String USER = System.getenv().getOrDefault("CHALLENGE_DB_USER", "sa");
    private static final String PASSWORD = System.getenv().getOrDefault("CHALLENGE_DB_PASSWORD", "");

    @Autowired private JdbcTemplate jdbc;
    @Autowired private ReservationService serviceA;
    @Autowired private EventRepository eventRepository;
    @Autowired private ReservationRepository reservationRepository;
    private ConfigurableApplicationContext second;
    private ReservationService serviceB;

    private static String databaseUrl() {
        String url = System.getenv().getOrDefault("CHALLENGE_JDBC_URL",
            "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=3000");
        if (!url.startsWith("jdbc:h2:mem:") &&
            !url.startsWith("jdbc:mysql://127.0.0.1:13316/challenge_day001?")) {
            throw new IllegalArgumentException("Use the documented disposable challenge DB only");
        }
        return url;
    }

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> URL);
        registry.add("spring.datasource.username", () -> USER);
        registry.add("spring.datasource.password", () -> PASSWORD);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.sql.init.mode", () -> "never");
        registry.add("spring.jpa.open-in-view", () -> "false");
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> "12");
    }

    @BeforeEach void setup() {
        // 전용 실습 DB 초기화/검증에만 JDBC를 사용합니다. 사용자 구현은 JPA입니다.
        jdbc.execute("DROP TABLE IF EXISTS reservation");
        jdbc.execute("DROP TABLE IF EXISTS event");
        new ResourceDatabasePopulator(new ClassPathResource("schema.sql")).execute(jdbc.getDataSource());
        second = new SpringApplicationBuilder(ChallengeApplication.class)
            .web(WebApplicationType.NONE)
            .properties(Map.of(
                "spring.datasource.url", URL,
                "spring.datasource.username", USER,
                "spring.datasource.password", PASSWORD,
                "spring.jpa.hibernate.ddl-auto", "none",
                "spring.sql.init.mode", "never",
                "spring.jpa.open-in-view", "false",
                "spring.datasource.hikari.maximum-pool-size", "12",
                "spring.main.banner-mode", "off"))
            .run();
        serviceB = second.getBean(ReservationService.class);
    }
    @AfterEach void close() {
        if (second != null) second.close();
    }

    @Test void jpaInfrastructureIsReady() {
        // 해답 검증이 아니라 엔티티 매핑과 서로 다른 Boot 컨텍스트의 DB 연결 확인.
        eventRepository.saveAndFlush(new Event(1L, 2));
        var reservation = reservationRepository.saveAndFlush(new Reservation(1L, 10L));
        var otherEvents = second.getBean(EventRepository.class);
        var otherReservations = second.getBean(ReservationRepository.class);
        // 연결/매핑 검사는 락 획득 검사가 아니므로 일반 조회를 사용한다.
        assertEquals(2, otherEvents.findById(1L).orElseThrow().getRemaining());
        assertEquals(10L, otherReservations.findById(reservation.getId()).orElseThrow().getUserId());
        assertNotSame(serviceA, serviceB);
        assertTrue(org.springframework.aop.support.AopUtils.isAopProxy(serviceA));
        assertTrue(org.springframework.aop.support.AopUtils.isAopProxy(serviceB));
    }

    private void seed(int capacity) { jdbc.update("INSERT INTO event(event_id, remaining) VALUES (1, ?)", capacity); }
    private int remaining() { return jdbc.queryForObject("SELECT remaining FROM event WHERE event_id=1", Integer.class); }
    private int count() { return jdbc.queryForObject("SELECT COUNT(*) FROM reservation", Integer.class); }

    @Test void successfulReservationPersists() {
        seed(2);
        var result = serviceA.reserve(1, 10);
        assertEquals(CREATED, result.status());
        assertNotNull(result.reservationId());
        assertEquals(1, remaining());
        assertEquals(1, count());
        assertEquals(10L, jdbc.queryForObject("SELECT user_id FROM reservation WHERE reservation_id=?", Long.class, result.reservationId()));
    }
    @Test void soldOutDoesNotChangeState() {
        seed(0);
        assertEquals(SOLD_OUT, serviceA.reserve(1, 10).status());
        assertEquals(0, remaining());
        assertEquals(0, count());
    }
    @Test void repeatedRequestReturnsOriginalEvenAfterSoldOut() {
        seed(1);
        var initial = serviceA.reserve(1, 10);
        var retry = serviceB.reserve(1, 10);
        assertEquals(CREATED, initial.status());
        assertEquals(EXISTING, retry.status());
        assertEquals(initial.reservationId(), retry.reservationId());
        assertEquals(0, remaining());
        assertEquals(1, count());
    }
    // 튜터 검증용: 이벤트 ID와 예약 ID가 우연히 같은 초기 데이터에 의존하지 않습니다.
    @Test void retryReturnsReservationIdWhenIdsDiffer() {
        seed(3);
        serviceA.reserve(1, 20);
        var original = serviceA.reserve(1, 10);
        assertNotEquals(1L, original.reservationId());
        var retry = serviceB.reserve(1, 10);
        assertEquals(EXISTING, retry.status());
        assertEquals(original.reservationId(), retry.reservationId());
        assertEquals(1, remaining());
        assertEquals(2, count());
    }

    @Test void storageFailureDoesNotConsumeSeat() {
        seed(2);
        // 실패 주입: 특정 테스트 사용자의 INSERT를 DB가 거부합니다.
        jdbc.execute("ALTER TABLE reservation ADD CONSTRAINT test_storage_failure CHECK (user_id <> 999)");
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> serviceA.reserve(1, 999));
        assertEquals(2, remaining());
        assertEquals(0, count());
    }
    @RepeatedTest(3) void lastSeatHasExactlyOneWinner() throws Exception {
        seed(1);
        var results = race(false);
        assertEquals(1L, results.stream().filter(r -> r.status() == CREATED).count());
        assertEquals(11L, results.stream().filter(r -> r.status() == SOLD_OUT).count());
        assertEquals(0, remaining());
        assertEquals(1, count());
    }
    @RepeatedTest(3) void simultaneousRetriesCreateOneReservation() throws Exception {
        seed(20);
        var results = race(true);
        assertEquals(1L, results.stream().filter(r -> r.status() == CREATED).count());
        assertEquals(11L, results.stream().filter(r -> r.status() == EXISTING).count());
        assertEquals(1L, results.stream().map(ReservationResult::reservationId).distinct().count());
        assertTrue(results.stream().allMatch(r -> r.reservationId() != null));
        assertEquals(19, remaining());
        assertEquals(1, count());
    }
    private List<ReservationResult> race(boolean sameUser) throws Exception {
        var pool = Executors.newFixedThreadPool(12);
        var ready = new CountDownLatch(12);
        var start = new CountDownLatch(1);
        var futures = new ArrayList<Future<ReservationResult>>();
        try {
            for (int i = 0; i < 12; i++) {
                final long user = sameUser ? 10 : i + 10;
                final var service = i % 2 == 0 ? serviceA : serviceB;
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    if (!start.await(5, TimeUnit.SECONDS)) throw new TimeoutException("start barrier");
                    return service.reserve(1, user);
                }));
            }
            assertTrue(ready.await(5, TimeUnit.SECONDS), "workers ready");
            start.countDown();
            var results = new ArrayList<ReservationResult>();
            for (var future : futures) results.add(future.get(10, TimeUnit.SECONDS));
            return results;
        } finally {
            start.countDown();
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS), "workers stopped");
        }
    }
}
