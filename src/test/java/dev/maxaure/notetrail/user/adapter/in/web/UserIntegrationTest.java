package dev.maxaure.notetrail.user.adapter.in.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "app.jwt.secret=test-secret-test-secret-test-secret-32"
)
@Testcontainers
class UserIntegrationTest {

    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void whenUserRegistersThenAccountIsPersistedWithBcryptHash() {
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/register",
                Map.of("login", "jan.kowalski", "password", "strong-pass"),
                String.class
        );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        String hash = jdbcTemplate.queryForObject(
                "select password from users where login = ?",
                String.class,
                "jan.kowalski"
        );
        assertNotNull(hash);
        assertNotEquals("strong-pass", hash);
        assertEquals(60, hash.length());
        assertTrue(hash.matches("^\\$2[aby]\\$\\d{2}\\$.{53}$"));
    }

    @Test
    void whenLoginIsAlreadyTakenThenClientGetsConflict() {
        restTemplate.postForEntity(
                "/register",
                Map.of("login", "anna.nowak", "password", "strong-pass"),
                String.class
        );

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/register",
                Map.of("login", "anna.nowak", "password", "strong-pass"),
                String.class
        );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    void whenSixthLoginAttemptComesThenClientGetsTooManyRequests() {
        for (int attempt = 1; attempt <= 5; attempt++) {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    "/login",
                    Map.of("login", "nobody", "password", "wrong-pass"),
                    String.class
            );

            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        }

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/login",
                Map.of("login", "nobody", "password", "wrong-pass"),
                String.class
        );

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        String retryAfter = response.getHeaders().getFirst("Retry-After");
        assertNotNull(retryAfter);
        assertTrue(Long.parseLong(retryAfter) > 0);
    }
}
