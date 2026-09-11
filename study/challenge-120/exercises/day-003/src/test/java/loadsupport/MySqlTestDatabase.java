package loadsupport;
import org.testcontainers.mysql.MySQLContainer;
/** 테스트 프로세스 전용 DB. 사용자 데이터베이스와 볼륨을 사용하지 않는다. */
public final class MySqlTestDatabase {
    private static final MySQLContainer DB = new MySQLContainer("mysql:8.4")
        .withDatabaseName("day003").withUsername("challenge").withPassword("challenge")
        .withCommand("--innodb-lock-wait-timeout=10");
    static { DB.start(); }
    public static String url() { return DB.getJdbcUrl(); }
    public static String user() { return DB.getUsername(); }
    public static String password() { return DB.getPassword(); }
    public static void stop() { DB.stop(); }
}
