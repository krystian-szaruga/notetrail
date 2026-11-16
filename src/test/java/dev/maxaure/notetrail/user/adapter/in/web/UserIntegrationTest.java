package dev.maxaure.notetrail.user.adapter.in.web;

import dev.maxaure.notetrail.MySqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.jwt.secret=test-secret-test-secret-test-secret-32",
                "app.ratelimit.login-capacity=1000",
                "notetrail.security.bcrypt-strength=4"
        }
)
class UserIntegrationTest extends MySqlIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ---------------------------------------------------------------
    // POST /register
    // ---------------------------------------------------------------

    @Test
    void whenUserRegistersThenAccountIsPersistedWithBcryptHash() {
        ResponseEntity<String> response = register("jan.kowalski", "strong-pass");

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
    void whenUserRegistersThenResponseContainsIdLoginAndCreatedAt() {
        ResponseEntity<RegisterResponse> response = restTemplate.postForEntity(
                "/register",
                Map.of("login", "shape.check", "password", "strong-pass"),
                RegisterResponse.class
        );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().id());
        assertEquals("shape.check", response.getBody().login());
        assertNotNull(Instant.parse(response.getBody().createdAt()));
    }

    @Test
    void whenRegisteredThenResponseDoesNotLeakPassword() {
        ResponseEntity<String> response = register("no.leak", "strong-pass");

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().contains("password"));
        assertFalse(response.getBody().contains("$2"));
    }

    @Test
    void whenLoginIsAlreadyTakenThenClientGetsConflict() {
        register("anna.nowak", "strong-pass");

        ResponseEntity<String> response = register("anna.nowak", "strong-pass");

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    void whenRegisterPayloadIsInvalidThenClientGetsBadRequest() {
        assertEquals(HttpStatus.BAD_REQUEST, register("ab", "strong-pass").getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, register("a".repeat(65), "strong-pass").getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, register("valid.login", "short").getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, register("valid.login2", "x".repeat(73)).getStatusCode());
    }

    @Test
    void whenRegisterFieldsAreMissingThenClientGetsBadRequest() {
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/register",
                Map.of("login", "only.login"),
                String.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void whenRegisterPasswordIsBlankThenClientGetsBadRequest() {
        assertEquals(HttpStatus.BAD_REQUEST, register("blank.pass", "").getStatusCode());
    }

    // ---------------------------------------------------------------
    // POST /login
    // ---------------------------------------------------------------

    @Test
    void whenCredentialsAreValidThenLoginReturnsToken() {
        register("login.ok", "strong-pass");

        ResponseEntity<TokenResponse> response = restTemplate.postForEntity(
                "/login",
                Map.of("login", "login.ok", "password", "strong-pass"),
                TokenResponse.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(3600, response.getBody().expiresIn());
        assertEquals(3, response.getBody().token().split("\\.").length);
    }

    @Test
    void whenPasswordIsWrongOrLoginUnknownThenResponsesAreIdentical() {
        register("known.user", "strong-pass");

        ResponseEntity<String> wrongPassword = login("known.user", "wrong-pass");
        ResponseEntity<String> unknownLogin = login("unknown.user", "wrong-pass");

        assertEquals(HttpStatus.UNAUTHORIZED, wrongPassword.getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, unknownLogin.getStatusCode());
        assertEquals(wrongPassword.getBody(), unknownLogin.getBody());
    }

    @Test
    void whenLoginPasswordExceedsBcryptLimitThenClientGetsUnauthorized() {
        assertEquals(HttpStatus.UNAUTHORIZED, login("anyone", "x".repeat(73)).getStatusCode());
    }

    @Test
    void whenLoginFieldsAreMissingOrBlankThenClientGetsBadRequest() {
        assertEquals(HttpStatus.BAD_REQUEST, postLogin(Map.of("login", "only.login")).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, postLogin(Map.of("password", "strong-pass")).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, postLogin(Map.of("login", "", "password", "strong-pass")).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, postLogin(Map.of("login", "someone", "password", "  ")).getStatusCode());
    }

    @Test
    void whenBodyIsMalformedJsonThenClientGetsBadRequest() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/login",
                new HttpEntity<>("{not-json", headers),
                String.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    private ResponseEntity<String> register(String login, String password) {
        return restTemplate.postForEntity(
                "/register",
                Map.of("login", login, "password", password),
                String.class
        );
    }

    private ResponseEntity<String> login(String login, String password) {
        return restTemplate.postForEntity(
                "/login",
                Map.of("login", login, "password", password),
                String.class
        );
    }

    private ResponseEntity<String> postLogin(Map<String, String> body) {
        return restTemplate.postForEntity("/login", body, String.class);
    }

    private record TokenResponse(String token, long expiresIn) {}

    private record RegisterResponse(UUID id, String login, String createdAt) {}
}