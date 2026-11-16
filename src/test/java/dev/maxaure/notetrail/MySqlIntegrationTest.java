package dev.maxaure.notetrail;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public abstract class MySqlIntegrationTest {

    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");

    static {
        MYSQL.start();
    }

    @BeforeEach
    void clearDomainTables() throws SQLException {
        try (Connection connection = DriverManager.getConnection(
                MYSQL.getJdbcUrl(),
                MYSQL.getUsername(),
                MYSQL.getPassword()
        )) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("SET FOREIGN_KEY_CHECKS = 0");
                for (String table : DOMAIN_TABLES) {
                    statement.execute("DELETE FROM `" + table + "`");
                }
                statement.execute("SET FOREIGN_KEY_CHECKS = 1");
            }
        }
    }

    private static final List<String> DOMAIN_TABLES = List.of(
            "item_permissions",
            "items",
            "users"
    );
}
