package loadsupport;

import challenge.reservation.concurrency.*;
import com.sun.net.httpserver.HttpServer;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.LongAdder;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

/** k6 전용 로컬 어댑터. 실제 서비스 프록시를 HTTP로 호출하며 main 코드에는 포함되지 않는다. */
public final class LoadTestServer {
    public static void main(String[] args) throws Exception {
        var context = new SpringApplicationBuilder(ChallengeApplication.class)
            .web(WebApplicationType.NONE).properties(Map.of(
                "spring.datasource.url", MySqlTestDatabase.url(),
                "spring.datasource.username", MySqlTestDatabase.user(),
                "spring.datasource.password", MySqlTestDatabase.password(),
                "spring.jpa.hibernate.ddl-auto", "none", "spring.sql.init.mode", "never",
                "spring.datasource.hikari.maximum-pool-size", "32",
                "spring.datasource.hikari.connection-timeout", "10000",
                "spring.main.banner-mode", "off")).run();
        var jdbc = context.getBean(JdbcTemplate.class);
        new ResourceDatabasePopulator(new ClassPathResource("reservation/reservation-concurrency/schema.sql")).execute(jdbc.getDataSource());
        jdbc.update("INSERT INTO event(event_id,remaining) VALUES(1,0)");
        jdbc.update("INSERT INTO reservation(reservation_id,event_id,user_id) VALUES(10,1,100)");
        var reserve = context.getBean(ReservationService.class);
        var cancel = context.getBean(CancellationService.class);
        var created = new LongAdder(); var cancelled = new LongAdder(); var errors = new LongAdder();
        var pool = Executors.newFixedThreadPool(128);
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1",0), 2048);
        server.setExecutor(pool);
        server.createContext("/", exchange -> {
            int status=200; String body;
            try {
                String path=exchange.getRequestURI().getPath();
                if(path.equals("/state") && exchange.getRequestMethod().equals("GET")) {
                    var snapshot=jdbc.queryForMap("SELECT remaining, (SELECT COUNT(*) FROM reservation WHERE event_id=1) AS reservations FROM event WHERE event_id=1");
                    var os=(com.sun.management.UnixOperatingSystemMXBean)ManagementFactory.getOperatingSystemMXBean();
                    String connections=jdbc.queryForMap("SHOW STATUS LIKE 'Threads_connected'").get("Value").toString();
                    body=String.format("{\"remaining\":%s,\"reservations\":%s,\"created\":%d,\"cancelled\":%d,\"errors\":%d,\"openFds\":%d,\"maxFds\":%d,\"mysqlConnections\":%s}",
                        snapshot.get("remaining"),snapshot.get("reservations"),created.sum(),cancelled.sum(),errors.sum(),os.getOpenFileDescriptorCount(),os.getMaxFileDescriptorCount(),connections);
                } else if(path.equals("/reserve") && exchange.getRequestMethod().equals("POST")) {
                    long user=Long.parseLong(exchange.getRequestURI().getRawQuery().replace("user=",""));
                    var result=reserve.reserve(1,user);
                    if(result.status()==ReservationResult.Status.CREATED) created.increment();
                    if(result.status()==ReservationResult.Status.SOLD_OUT) status=409;
                    body=String.format("{\"status\":\"%s\",\"reservationId\":%s}",result.status(),result.reservationId());
                } else if(path.equals("/cancel") && exchange.getRequestMethod().equals("POST")) {
                    long id=Long.parseLong(exchange.getRequestURI().getRawQuery().replace("id=",""));
                    var result=cancel.cancel(1,id);
                    if(result==CancellationService.Result.CANCELLED) cancelled.increment();
                    body="{\"status\":\""+result+"\"}";
                } else { status=404; body="{\"error\":\"not_found\"}"; }
            } catch(Exception failure) {
                errors.increment(); status=500; body="{\"error\":\""+failure.getClass().getSimpleName()+"\"}";
                failure.printStackTrace();
            }
            byte[] bytes=body.getBytes(StandardCharsets.UTF_8);
            try {
                exchange.getResponseHeaders().set("Content-Type","application/json");
                exchange.sendResponseHeaders(status,bytes.length);
                exchange.getResponseBody().write(bytes);
            } finally { exchange.close(); }
        });
        server.start();
        Path ready=Path.of("build/load-server.json"); Files.createDirectories(ready.getParent());
        Files.writeString(ready,String.format("{\"url\":\"http://127.0.0.1:%d\",\"pid\":%d,\"database\":\"%s\"}",
            server.getAddress().getPort(),ProcessHandle.current().pid(),jdbc.queryForObject("SELECT VERSION()",String.class)));
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.stop(1); pool.shutdownNow(); context.close(); MySqlTestDatabase.stop();
            try { Files.deleteIfExists(ready); } catch(Exception ignored) {}
        }));
        new CountDownLatch(1).await();
    }
}
