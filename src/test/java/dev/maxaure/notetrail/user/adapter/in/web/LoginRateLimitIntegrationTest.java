package dev.maxaure.notetrail.user.adapter.in.web;


import dev.maxaure.notetrail.MySqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.jwt.secret=test-secret-test-secret-test-secret-32",
                "app.ratelimit.login-capacity=5",
                "app.ratelimit.login-refill-seconds=60",
                "notetrail.security.bcrypt-strength=4"
        }
)
class LoginRateLimitIntegrationTest extends MySqlIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void whenSixthLoginAttemptComesThenClientGetsTooManyRequests() {
        for (int attempt = 1; attempt <= 5; attempt++) {
            ResponseEntity<String> response = login();

            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        }

        ResponseEntity<String> response = login();

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        String retryAfter = response.getHeaders().getFirst("Retry-After");
        assertNotNull(retryAfter);
        long seconds = Long.parseLong(retryAfter);
        assertTrue(seconds > 0 && seconds <= 60);
    }

    private ResponseEntity<String> login() {
        return restTemplate.postForEntity(
                "/login",
                Map.of("login", "nobody", "password", "wrong-pass"),
                String.class
        );
    }
}