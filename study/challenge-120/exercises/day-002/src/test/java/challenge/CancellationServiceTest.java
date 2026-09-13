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
import static challenge.CancellationService.Result.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = ChallengeApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Timeout(30)
class CancellationServiceTest {
    private static final String URL = databaseUrl();
    private static final String USER = "sa";
    private static final String PASSWORD = "";

    @Autowired private JdbcTemplate jdbc;
    @Autowired private CancellationService serviceA;
    @Autowired private EventRepository eventRepository;
    @Autowired private ReservationRepository reservationRepository;
    private ConfigurableApplicationContext second;
    private CancellationService serviceB;

    private static String databaseUrl() {
        String url = "jdbc:h2:mem:day002_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=3000";
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
        serviceB = second.getBean(CancellationService.class);
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

    private void seed(int remaining, int reservations) {
        jdbc.update("INSERT INTO event(event_id, remaining) VALUES (1, ?)", remaining);
        for(int i=0;i<reservations;i++) jdbc.update("INSERT INTO reservation(reservation_id,event_id,user_id) VALUES (?,1,?)",10+i,100+i);
    }
    private int remaining() { return jdbc.queryForObject("SELECT remaining FROM event WHERE event_id=1",Integer.class); }
    private int count() { return jdbc.queryForObject("SELECT COUNT(*) FROM reservation",Integer.class); }
    @Test void cancellationPersists() {
        seed(0,1); assertEquals(CANCELLED,serviceA.cancel(1,10)); assertEquals(1,remaining()); assertEquals(0,count());
    }
    @Test void retryDoesNotReturnAnotherSeat() {
        seed(0,1); assertEquals(CANCELLED,serviceA.cancel(1,10)); assertEquals(NOT_FOUND,serviceB.cancel(1,10)); assertEquals(1,remaining()); assertEquals(0,count());
    }
    @Test void unknownReservationDoesNotChangeState() {
        seed(0,1); assertEquals(NOT_FOUND,serviceA.cancel(1,99)); assertEquals(0,remaining()); assertEquals(1,count());
    }
    @Test void anotherEventsReservationIsUntouched() {
        seed(0,1); jdbc.update("INSERT INTO event(event_id,remaining) VALUES (2,4)");
        assertEquals(NOT_FOUND,serviceA.cancel(2,10)); assertEquals(0,remaining()); assertEquals(1,count());
        assertEquals(4,jdbc.queryForObject("SELECT remaining FROM event WHERE event_id=2",Integer.class));
    }
    @Test void storageFailureRollsBackBothChanges() {
        seed(0,1); jdbc.execute("ALTER TABLE event ADD CONSTRAINT injected_failure CHECK (remaining = 0)");
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->serviceA.cancel(1,10));
        assertEquals(0,remaining()); assertEquals(1,count());
    }
    @RepeatedTest(3) @Tag("concurrency") void sameReservationReturnsSeatOnce() throws Exception {
        seed(0,1); var results=race(true);
        assertEquals(1,results.stream().filter(r->r==CANCELLED).count());
        assertEquals(7,results.stream().filter(r->r==NOT_FOUND).count());
        assertEquals(1,remaining()); assertEquals(0,count());
    }
    @RepeatedTest(3) @Tag("concurrency") void differentReservationsReturnBothSeats() throws Exception {
        seed(0,2); var results=race(false);
        assertTrue(results.stream().allMatch(r->r==CANCELLED)); assertEquals(2,remaining()); assertEquals(0,count());
    }
    private List<CancellationService.Result> race(boolean same) throws Exception {
        int n=same?8:2;
        var pool=Executors.newFixedThreadPool(n); var ready=new CountDownLatch(n); var start=new CountDownLatch(1);
        var futures=new ArrayList<Future<CancellationService.Result>>();
        try {
            for(int i=0;i<n;i++) {
                long id=same?10:10+i; var service=i%2==0?serviceA:serviceB;
                futures.add(pool.submit(()->{ready.countDown(); if(!start.await(5,TimeUnit.SECONDS)) throw new TimeoutException("start"); return service.cancel(1,id);}));
            }
            assertTrue(ready.await(5,TimeUnit.SECONDS)); start.countDown();
            var results=new ArrayList<CancellationService.Result>();
            for(var f:futures) results.add(f.get(10,TimeUnit.SECONDS));
            return results;
        } finally { start.countDown(); pool.shutdownNow(); assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS)); }
    }
}
