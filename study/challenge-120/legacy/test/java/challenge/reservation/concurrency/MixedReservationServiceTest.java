package challenge.reservation.concurrency;

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
import static challenge.reservation.concurrency.MixedReservationServiceTest.Result.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = ChallengeApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Timeout(30)
class MixedReservationServiceTest {
    enum Result { RESERVED, EXISTING, SOLD_OUT, CANCELLED, NOT_FOUND }
    // 테스트 결과 비교용 변환. 서비스의 기존 반환 타입과 예약ID는 그대로 유지한다.
    private Result reserve(ReservationService service, long eventId, long userId) {
        var result = service.reserve(eventId,userId);
        if (result.status()==ReservationResult.Status.CREATED || result.status()==ReservationResult.Status.EXISTING)
            assertNotNull(result.reservationId());
        else assertNull(result.reservationId());
        return result.status()==ReservationResult.Status.CREATED ? RESERVED : Result.valueOf(result.status().name());
    }
    private Result cancel(CancellationService service, long eventId, long reservationId) {
        return Result.valueOf(service.cancel(eventId,reservationId).name());
    }
    private static final boolean MYSQL = Boolean.getBoolean("challenge.mysql");
    private static final String URL = MYSQL ? loadsupport.MySqlTestDatabase.url() : databaseUrl();
    private static final String USER = MYSQL ? loadsupport.MySqlTestDatabase.user() : "sa";
    private static final String PASSWORD = MYSQL ? loadsupport.MySqlTestDatabase.password() : "";

    @Autowired private JdbcTemplate jdbc;
    @Autowired private ReservationService serviceA;
    @Autowired private CancellationService cancellationA;
    @Autowired private EventRepository eventRepository;
    @Autowired private ReservationRepository reservationRepository;
    private ConfigurableApplicationContext second;
    private ReservationService serviceB;
    private CancellationService cancellationB;

    private static String databaseUrl() {
        String url = "jdbc:h2:mem:day003_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=3000";
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
        new ResourceDatabasePopulator(new ClassPathResource("reservation/reservation-concurrency/schema.sql")).execute(jdbc.getDataSource());
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
        cancellationB = second.getBean(CancellationService.class);
    }
    @AfterEach void close() {
        if (second != null) second.close();
    }

    @Test void jpaInfrastructureIsReady() {
        // 해답 검증이 아니라 엔티티 매핑과 서로 다른 Boot 컨텍스트의 DB 연결 확인.
        eventRepository.saveAndFlush(new Event(1L, 0));
        var reservation = reservationRepository.saveAndFlush(new Reservation(1L, 10L));
        var otherEvents = second.getBean(EventRepository.class);
        var otherReservations = second.getBean(ReservationRepository.class);
        // 연결/매핑 검사는 락 획득 검사가 아니므로 일반 조회를 사용한다.
        assertEquals(0, otherEvents.findById(1L).orElseThrow().getRemaining());
        assertEquals(10L, otherReservations.findById(reservation.getId()).orElseThrow().getUserId());
        assertNotSame(serviceA, serviceB);
        assertTrue(org.springframework.aop.support.AopUtils.isAopProxy(serviceA));
        assertTrue(org.springframework.aop.support.AopUtils.isAopProxy(serviceB));
    }

    private void seed(boolean full) {
        jdbc.update("INSERT INTO event(event_id,remaining) VALUES (1,?)",full?0:1);
        if(full) jdbc.update("INSERT INTO reservation(reservation_id,event_id,user_id) VALUES (10,1,100)");
    }
    private void state(int oldCount,int newCount,int remaining) {
        assertEquals(oldCount,jdbc.queryForObject("SELECT COUNT(*) FROM reservation WHERE user_id=100",Integer.class));
        assertEquals(newCount,jdbc.queryForObject("SELECT COUNT(*) FROM reservation WHERE user_id=200",Integer.class));
        assertEquals(remaining,jdbc.queryForObject("SELECT remaining FROM event WHERE event_id=1",Integer.class));
        assertEquals(1,remaining+jdbc.queryForObject("SELECT COUNT(*) FROM reservation",Integer.class));
    }
    @Test void cancelCommittedBeforeReserveStarts() {
        seed(true); assertEquals(CANCELLED,cancel(cancellationA,1,10));
        assertEquals(RESERVED,reserve(serviceB,1,200)); state(0,1,0);
    }
    @Test void reserveCommittedBeforeCancelStarts() {
        seed(true); assertEquals(SOLD_OUT,reserve(serviceA,1,200));
        assertEquals(CANCELLED,cancel(cancellationB,1,10)); state(0,0,1);
    }
    @Test void cancelRetryDoesNotReturnTwice() {
        seed(true); assertEquals(CANCELLED,cancel(cancellationA,1,10));
        assertEquals(NOT_FOUND,cancel(cancellationB,1,10)); state(0,0,1);
    }
    @Test void missingAndOtherEventAreUnchanged() {
        seed(true); jdbc.update("INSERT INTO event(event_id,remaining) VALUES (2,1)");
        assertEquals(NOT_FOUND,cancel(cancellationA,2,10));
        assertEquals(NOT_FOUND,cancel(cancellationA,1,99));
        assertEquals(NOT_FOUND,cancel(cancellationA,99,10));
        state(1,0,0);
        assertEquals(1,jdbc.queryForObject("SELECT remaining FROM event WHERE event_id=2",Integer.class));
    }
    @Test void missingEventReservationReturnsNotFound() {
        seed(true);
        assertEquals(NOT_FOUND,reserve(serviceA,99,200)); state(1,0,0);
    }
    @Test void existingReservationKeepsOriginalIdWithoutAnotherDeduction() {
        seed(true);
        var result=serviceA.reserve(1,100);
        assertEquals(ReservationResult.Status.EXISTING,result.status());
        assertEquals(10L,result.reservationId()); state(1,0,0);
    }
    @RepeatedTest(5) @Tag("concurrency") void concurrentCallsUseServicesOwnTransactions() throws Exception {
        seed(true);
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        var c=workers.submit(() -> { ready.countDown(); await(start); return cancel(cancellationA,1,10); });
        var next=workers.submit(() -> { ready.countDown(); await(start); return reserve(serviceB,1,200); });
        try {
            await(ready); start.countDown();
            org.awaitility.Awaitility.await().alias("예약·취소 요청 완료").atMost(10,TimeUnit.SECONDS)
                .until(() -> c.isDone() && next.isDone());
            assertEquals(CANCELLED,result(c)); assertAllowedReservation(result(next));
        } finally { start.countDown(); }
    }
    // 아래는 전부 테스트 전용이다. 서비스는 Repository와 @Transactional만 사용한다.
    // 순차 테스트는 서비스 자체 트랜잭션으로, 제어된 경합 테스트는 각 worker의 별도
    // 트랜잭션에 서비스(REQUIRED)가 참여하도록 실행한다. 테스트 메서드 전체는 트랜잭션이 아니다.
    private final ExecutorService workers = Executors.newFixedThreadPool(2);
    @Autowired private org.springframework.transaction.PlatformTransactionManager managerA;

    @AfterEach void stopWorkers() throws Exception {
        workers.shutdownNow();
        assertTrue(workers.awaitTermination(5, TimeUnit.SECONDS));
    }

    static class InjectedFailure extends RuntimeException {}
    private static void await(CountDownLatch latch) {
        try { assertTrue(latch.await(5, TimeUnit.SECONDS), "테스트 동기화 지점 미도달"); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new AssertionError(e); }
    }
    static class Gate {
        final CountDownLatch reached = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        void pause() { reached.countDown(); await(release); }
    }
    private org.springframework.transaction.PlatformTransactionManager managerB() {
        return second.getBean(org.springframework.transaction.PlatformTransactionManager.class);
    }
    private Result inTransaction(
            org.springframework.transaction.PlatformTransactionManager manager,
            java.util.function.Supplier<Result> action,
            Runnable before, Runnable after) {
        var tx = new org.springframework.transaction.support.TransactionTemplate(manager);
        tx.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return tx.execute(status -> {
            before.run();
            var result = action.get();
            after.run();
            return result;
        });
    }
    private Future<Result> async(
            org.springframework.transaction.PlatformTransactionManager manager,
            java.util.function.Supplier<Result> action,
            Runnable before, Runnable after) {
        return workers.submit(() -> inTransaction(manager, action, before, after));
    }
    private static Result result(Future<Result> future) throws Exception {
        return future.get(10, TimeUnit.SECONDS);
    }
    private static void reached(Gate gate, Future<Result> future) throws Exception {
        org.awaitility.Awaitility.await().alias("커밋 전 도달 또는 요청 종료").atMost(5,TimeUnit.SECONDS)
            .until(() -> gate.reached.getCount()==0 || future.isDone());
        if (gate.reached.getCount()!=0) {
            result(future);
            fail("서비스 반환 이후 지점 미도달");
        }
    }
    private void flushA() { eventRepository.flush(); }
    private void flushB() { second.getBean(EventRepository.class).flush(); }

    @Test void testHarnessSeesUncommittedWriteAndRollback() throws Exception {
        seed(false);
        var gate = new Gate();
        var pending = async(managerB(), () -> {
            assertTrue(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive());
            // 테스트 인프라만 검사하는 단일 쓰기. 예약/취소 업무 구현은 아니다.
            second.getBean(JdbcTemplate.class).update("UPDATE event SET remaining=0 WHERE event_id=1");
            return RESERVED;
        }, () -> {}, () -> { gate.pause(); throw new InjectedFailure(); });
        try {
            reached(gate, pending);
            state(0,0,1);
            gate.release.countDown();
            var error = assertThrows(ExecutionException.class, () -> result(pending));
            assertInstanceOf(InjectedFailure.class, error.getCause());
            state(0,0,1);
        } finally { gate.release.countDown(); }
    }
    @Test void cancellationFailureRollsBack() {
        seed(true);
        assertThrows(InjectedFailure.class, () -> inTransaction(managerB(), () -> cancel(cancellationB,1,10),
            () -> {}, () -> { flushB(); throw new InjectedFailure(); }));
        state(1,0,0);
    }
    @Test void reservationFailureRollsBack() {
        seed(false);
        assertThrows(InjectedFailure.class, () -> inTransaction(managerB(), () -> reserve(serviceB,1,200),
            () -> {}, () -> { flushB(); throw new InjectedFailure(); }));
        state(0,0,1);
    }
    @Test @Tag("concurrency") void bothTransactionsEnterTogether() throws Exception {
        seed(true);
        var entered = new CountDownLatch(2);
        Runnable barrier = () -> { entered.countDown(); await(entered); };
        var c = async(managerA, () -> cancel(cancellationA,1,10), barrier, () -> {});
        var r = async(managerB(), () -> reserve(serviceB,1,200), barrier, () -> {});
        assertEquals(CANCELLED, result(c));
        assertAllowedReservation(result(r));
    }
    @Test @Tag("concurrency") void cancellationUncommittedWhenReservationStarts() throws Exception {
        cancellationOverlap(false);
    }
    @Test @Tag("concurrency") void cancellationRollsBackWhileReservationIsInFlight() throws Exception {
        cancellationOverlap(true);
    }
    private void cancellationOverlap(boolean failAfterWork) throws Exception {
        seed(true);
        var gate = new Gate();
        var reserveEntered = new CountDownLatch(1);
        var c = async(managerA, () -> cancel(cancellationA,1,10), () -> {}, () -> {
            flushA(); gate.pause();
            if (failAfterWork) throw new InjectedFailure();
        });
        try {
            reached(gate,c);
            state(1,0,0);
            var r = async(managerB(), () -> reserve(serviceB,1,200), reserveEntered::countDown, () -> {});
            await(reserveEntered);
            gate.release.countDown();
            if (failAfterWork) {
                var error = assertThrows(ExecutionException.class, () -> result(c));
                assertInstanceOf(InjectedFailure.class, error.getCause());
                assertEquals(SOLD_OUT,result(r)); state(1,0,0);
            } else {
                assertEquals(CANCELLED,result(c)); assertAllowedReservation(result(r));
            }
        } finally { gate.release.countDown(); }
    }
    @Test @Tag("concurrency") void soldOutDecisionPrecedesCancellationEntry() throws Exception {
        seed(true);
        var gate = new Gate();
        var cancelEntered = new CountDownLatch(1);
        var r = async(managerA, () -> reserve(serviceA,1,200), () -> {}, gate::pause);
        try {
            reached(gate,r);
            var c = async(managerB(), () -> cancel(cancellationB,1,10), cancelEntered::countDown, () -> {});
            await(cancelEntered);
            gate.release.countDown();
            assertEquals(SOLD_OUT,result(r)); assertEquals(CANCELLED,result(c)); state(0,0,1);
        } finally { gate.release.countDown(); }
    }
    private void assertAllowedReservation(Result value) {
        assertTrue(value==RESERVED || value==SOLD_OUT,"예외/NOT_FOUND는 허용 응답이 아닙니다");
        if(value==RESERVED) state(0,1,0); else state(0,0,1);
    }
}
