package dev.maxaure.notetrail.item.adapter.in.web;

import dev.maxaure.notetrail.MySqlIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;

import java.util.Arrays;
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
@AutoConfigureTestRestTemplate
abstract class ItemApiTestSupport extends MySqlIntegrationTest {

    protected static final String PASSWORD = "strong-pass";

    @Autowired
    protected TestRestTemplate restTemplate;

    protected Session registerAndLogin(String login) {
        ResponseEntity<RegisterBody> registered = restTemplate.postForEntity(
                "/register",
                Map.of("login", login, "password", PASSWORD),
                RegisterBody.class
        );
        assertEquals(HttpStatus.CREATED, registered.getStatusCode());
        assertNotNull(registered.getBody());

        ResponseEntity<TokenBody> loggedIn = restTemplate.postForEntity(
                "/login",
                Map.of("login", login, "password", PASSWORD),
                TokenBody.class
        );
        assertEquals(HttpStatus.OK, loggedIn.getStatusCode());
        assertNotNull(loggedIn.getBody());
        return new Session(loggedIn.getBody().token(), registered.getBody().id(), login);
    }

    protected ItemBody createItem(String token, String title) {
        ResponseEntity<ItemBody> response = create(
                token,
                Map.of("title", title, "content", "treść")
        );
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        return response.getBody();
    }

    protected ResponseEntity<ItemBody> create(String token, Map<String, Object> body) {
        return restTemplate.exchange(
                "/items",
                HttpMethod.POST,
                new HttpEntity<>(body, authHeaders(token)),
                ItemBody.class
        );
    }

    protected ResponseEntity<ItemPatchBody> patch(String token, UUID itemId, Map<String, Object> body) {
        return restTemplate.exchange(
                "/items/" + itemId,
                HttpMethod.PATCH,
                new HttpEntity<>(body, authHeaders(token)),
                ItemPatchBody.class
        );
    }

    protected ResponseEntity<Void> delete(String token, UUID itemId) {
        return restTemplate.exchange(
                "/items/" + itemId,
                HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(token)),
                Void.class
        );
    }

    protected ResponseEntity<HistoryBody[]> history(String token, UUID itemId) {
        return restTemplate.exchange(
                "/items/" + itemId + "/history",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                HistoryBody[].class
        );
    }

    protected HttpStatusCode historyStatus(String token, UUID itemId) {
        return restTemplate.exchange(
                "/items/" + itemId + "/history",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                String.class
        ).getStatusCode();
    }

    protected ResponseEntity<ShareBody> share(String token, UUID itemId, UUID userId, String role) {
        return restTemplate.exchange(
                "/items/" + itemId + "/share",
                HttpMethod.POST,
                new HttpEntity<>(
                        Map.of("userId", userId.toString(), "role", role),
                        authHeaders(token)
                ),
                ShareBody.class
        );
    }

    protected ResponseEntity<Void> revoke(String token, UUID itemId, UUID userId) {
        return restTemplate.exchange(
                "/items/" + itemId + "/share/" + userId,
                HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(token)),
                Void.class
        );
    }

    protected boolean listContains(String token, UUID itemId) {
        ResponseEntity<ItemSummaryBody[]> response = restTemplate.exchange(
                "/items",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                ItemSummaryBody[].class
        );
        assertNotNull(response.getBody());
        return Arrays.stream(response.getBody()).anyMatch(summary -> summary.id().equals(itemId));
    }

    protected ItemSummaryBody findInList(String token, UUID itemId) {
        ResponseEntity<ItemSummaryBody[]> response = restTemplate.exchange(
                "/items",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                ItemSummaryBody[].class
        );
        assertNotNull(response.getBody());
        return Arrays.stream(response.getBody())
                .filter(summary -> summary.id().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("item not found in list: " + itemId));
    }

    protected HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return headers;
    }

    protected ResponseEntity<String> withoutToken(HttpMethod method, String url, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.exchange(url, method, new HttpEntity<>(body, headers), String.class);
    }

    protected static String uniqueLogin(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    protected record Session(String token, UUID id, String login) {
    }

    protected record RegisterBody(UUID id, String login, String createdAt) {
    }

    protected record TokenBody(String token, long expiresIn) {
    }

    protected record ItemBody(
            UUID id,
            String title,
            String content,
            Integer version,
            UUID ownerId,
            String createdAt,
            String updatedAt
    ) {
    }

    protected record ItemPatchBody(
            UUID id,
            String title,
            String content,
            Integer version,
            String updatedAt
    ) {
    }

    protected record ItemSummaryBody(
            UUID id,
            String title,
            String content,
            int version,
            UUID ownerId,
            String myRole,
            String updatedAt
    ) {
    }

    protected record HistoryBody(
            int revision,
            String revisionType,
            String timestamp,
            String changedBy,
            String title,
            String content
    ) {
    }

    protected record ShareBody(UUID itemId, UUID userId, String role, String grantedAt) {
    }

    protected record ProblemBody(int status, String detail, Integer version) {
    }
}
