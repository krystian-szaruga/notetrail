package dev.maxaure.notetrail.item.adapter.in.web;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.*;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.jwt.secret=test-secret-test-secret-test-secret-32",
                "app.ratelimit.login-capacity=1000",
                "notetrail.security.bcrypt-strength=4"
        }
)
@Testcontainers
class ItemIntegrationTest {

    private static final String PASSWORD = "strong-pass";

    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void whenItemIsCreatedThenResponseContainsInitialVersionAndOwner() {
        Session alice = registerAndLogin(uniqueLogin("alice"));

        ItemBody item = createItem(alice.token(), "pierwsza");

        assertEquals("pierwsza", item.title());
        assertEquals("treść", item.content());
        assertEquals(0, item.version());
        assertEquals(alice.id(), item.ownerId());
        assertNotNull(item.createdAt());
        assertNotNull(item.updatedAt());
    }

    @Test
    void whenOwnerEditsTwiceThenHistoryContainsAllRevisions() {
        Session alice = registerAndLogin(uniqueLogin("alice"));
        ItemBody item = createItem(alice.token(), "pierwsza");

        ItemBody firstEdit = patch(
                alice.token(),
                item.id(),
                Map.of("title", "druga", "version", 0)
        ).getBody();
        assertNotNull(firstEdit);
        assertEquals(1, firstEdit.version());

        ItemBody secondEdit = patch(alice.token(), item.id(), Map.of(
                "content", "nowa treść",
                "version", firstEdit.version()
        )).getBody();
        assertNotNull(secondEdit);
        assertEquals(2, secondEdit.version());

        ResponseEntity<HistoryBody[]> response = history(alice.token(), item.id());
        assertEquals(HttpStatus.OK, response.getStatusCode());

        HistoryBody[] revisions = response.getBody();
        assertNotNull(revisions);
        assertEquals(3, revisions.length);
        assertEquals("ADD", revisions[0].revisionType());
        assertEquals("MOD", revisions[1].revisionType());
        assertEquals("MOD", revisions[2].revisionType());
        assertEquals("druga", revisions[2].title());
        assertEquals("nowa treść", revisions[2].content());
        Arrays.stream(revisions)
                .forEach(revision -> assertEquals(alice.login(), revision.changedBy()));
    }

    @Test
    void whenVersionIsStaleThenConflictContainsCurrentVersion() {
        Session alice = registerAndLogin(uniqueLogin("alice"));
        ItemBody item = createItem(alice.token(), "pierwsza");
        patch(alice.token(), item.id(), Map.of("title", "druga", "version", 0));

        ResponseEntity<ProblemBody> response = restTemplate.exchange(
                "/items/" + item.id(),
                HttpMethod.PATCH,
                new HttpEntity<>(
                        Map.of("title", "trzecia", "version", 0),
                        authHeaders(alice.token())
                ),
                ProblemBody.class
        );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().version());
    }

    @Test
    void whenItemIsSoftDeletedThenItDisappearsAndIsNotFound() {
        Session alice = registerAndLogin(uniqueLogin("alice"));
        ItemBody item = createItem(alice.token(), "do usunięcia");

        assertEquals(
                HttpStatus.NO_CONTENT,
                delete(alice.token(), item.id()).getStatusCode()
        );
        assertFalse(listContains(alice.token(), item.id()));
        assertEquals(
                HttpStatus.NOT_FOUND,
                patch(
                        alice.token(),
                        item.id(),
                        Map.of("title", "po", "version", 1)
                ).getStatusCode()
        );
        assertEquals(HttpStatus.NOT_FOUND, historyStatus(alice.token(), item.id()));
        assertEquals(
                HttpStatus.NOT_FOUND,
                delete(alice.token(), item.id()).getStatusCode()
        );
    }

    @Test
    void whenRequestHasNoTokenThenUnauthorizedIsReturned() {
        assertEquals(
                HttpStatus.UNAUTHORIZED,
                restTemplate.getForEntity("/items", String.class).getStatusCode()
        );
    }

    @Test
    void whenStrangerAccessesItemThenForbiddenIsReturned() {
        Session alice = registerAndLogin(uniqueLogin("alice"));
        Session bob = registerAndLogin(uniqueLogin("bob"));
        ItemBody item = createItem(alice.token(), "prywatna");

        assertEquals(
                HttpStatus.FORBIDDEN,
                patch(
                        bob.token(),
                        item.id(),
                        Map.of("title", "hack", "version", 0)
                ).getStatusCode()
        );
        assertEquals(
                HttpStatus.FORBIDDEN,
                delete(bob.token(), item.id()).getStatusCode()
        );
        assertEquals(HttpStatus.FORBIDDEN, historyStatus(bob.token(), item.id()));
        assertEquals(
                HttpStatus.FORBIDDEN,
                share(bob.token(), item.id(), bob.id(), "VIEWER").getStatusCode()
        );
        assertEquals(
                HttpStatus.FORBIDDEN,
                revoke(bob.token(), item.id(), bob.id()).getStatusCode()
        );
    }

    @Test
    void whenItemIsSharedThenEditorCanEditAndHistoryShowsEditor() {
        Session alice = registerAndLogin(uniqueLogin("alice"));
        Session bob = registerAndLogin(uniqueLogin("bob"));
        ItemBody item = createItem(alice.token(), "wspólna");

        ResponseEntity<ShareBody> shared = share(alice.token(), item.id(), bob.id(), "EDITOR");
        assertEquals(HttpStatus.CREATED, shared.getStatusCode());
        assertNotNull(shared.getBody());
        assertEquals("EDITOR", shared.getBody().role());

        ItemSummaryBody summary = findInList(bob.token(), item.id());
        assertEquals("EDITOR", summary.myRole());

        assertEquals(HttpStatus.OK, patch(bob.token(), item.id(), Map.of(
                "content", "edit by bob", "version", 0)).getStatusCode()
        );

        HistoryBody[] revisions = history(bob.token(), item.id()).getBody();
        assertNotNull(revisions);
        assertEquals(2, revisions.length);
        assertEquals(bob.login(), revisions[1].changedBy());
    }

    @Test
    void whenShareRoleIsOverwrittenThenResponseIsOkAndGrantedAtIsKept() {
        Session alice = registerAndLogin(uniqueLogin("alice"));
        Session bob = registerAndLogin(uniqueLogin("bob"));
        ItemBody item = createItem(alice.token(), "wspólna");

        ShareBody first = share(
                alice.token(),
                item.id(),
                bob.id(),
                "VIEWER"
        ).getBody();
        assertNotNull(first);

        ResponseEntity<ShareBody> response = share(
                alice.token(),
                item.id(),
                bob.id(),
                "EDITOR"
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("EDITOR", response.getBody().role());
        assertEquals(
                Instant.parse(first.grantedAt()).truncatedTo(ChronoUnit.SECONDS),
                Instant.parse(response.getBody().grantedAt()).truncatedTo(ChronoUnit.SECONDS)
        );
    }

    @Test
    void whenItemIsSharedWithOwnerThenValidationFails() {
        Session alice = registerAndLogin(uniqueLogin("alice"));
        ItemBody item = createItem(alice.token(), "wspólna");

        assertEquals(
                HttpStatus.BAD_REQUEST,
                share(
                        alice.token(),
                        item.id(),
                        alice.id(),
                        "VIEWER"
                ).getStatusCode()
        );
    }

    @Test
    void whenRoleIsOwnerThenValidationFails() {
        Session alice = registerAndLogin(uniqueLogin("alice"));
        Session bob = registerAndLogin(uniqueLogin("bob"));
        ItemBody item = createItem(alice.token(), "wspólna");

        assertEquals(
                HttpStatus.BAD_REQUEST,
                share(
                        alice.token(),
                        item.id(),
                        bob.id(),
                        "OWNER"
                ).getStatusCode()
        );
    }

    @Test
    void whenShareTargetDoesNotExistThenNotFoundIsReturned() {
        Session alice = registerAndLogin(uniqueLogin("alice"));
        ItemBody item = createItem(alice.token(), "wspólna");

        assertEquals(
                HttpStatus.NOT_FOUND,
                share(
                        alice.token(),
                        item.id(),
                        UUID.randomUUID(),
                        "VIEWER"
                ).getStatusCode()
        );
    }

    @Test
    void whenViewerReadsThenListAndHistoryAreAvailableButWritesAreForbidden() {
        Session alice = registerAndLogin(uniqueLogin("alice"));
        Session bob = registerAndLogin(uniqueLogin("bob"));
        ItemBody item = createItem(alice.token(), "tylko odczyt");

        assertEquals(
                HttpStatus.CREATED,
                share(alice.token(), item.id(), bob.id(), "VIEWER").getStatusCode()
        );

        assertEquals("VIEWER", findInList(bob.token(), item.id()).myRole());
        assertEquals(HttpStatus.OK, history(bob.token(), item.id()).getStatusCode());
        assertEquals(
                HttpStatus.FORBIDDEN,
                patch(
                        bob.token(),
                        item.id(),
                        Map.of("title", "hack", "version", 0)
                ).getStatusCode()
        );
        assertEquals(
                HttpStatus.FORBIDDEN,
                delete(bob.token(), item.id()).getStatusCode()
        );
        assertEquals(
                HttpStatus.FORBIDDEN,
                share(bob.token(), item.id(), bob.id(), "EDITOR").getStatusCode()
        );
    }

    @Test
    void whenAccessIsRevokedThenUserLosesIt() {
        Session alice = registerAndLogin(uniqueLogin("alice"));
        Session bob = registerAndLogin(uniqueLogin("bob"));
        ItemBody item = createItem(alice.token(), "wspólna");

        share(alice.token(), item.id(), bob.id(), "EDITOR");

        assertEquals(
                HttpStatus.NO_CONTENT,
                revoke(alice.token(), item.id(), bob.id()).getStatusCode()
        );
        assertFalse(listContains(bob.token(), item.id()));
        assertEquals(
                HttpStatus.FORBIDDEN,
                patch(
                        bob.token(),
                        item.id(),
                        Map.of("title", "po", "version", 0)
                ).getStatusCode()
        );
        assertEquals(
                HttpStatus.NOT_FOUND,
                revoke(alice.token(), item.id(), bob.id()).getStatusCode()
        );
    }

    @Test
    void whenOwnerListsItemsThenMyRoleIsOwner() {
        Session alice = registerAndLogin(uniqueLogin("alice"));
        ItemBody item = createItem(alice.token(), "moja");

        assertEquals("OWNER", findInList(alice.token(), item.id()).myRole());
    }

    @Test
    void whenRequestIsInvalidThenBadRequestIsReturned() {
        Session alice = registerAndLogin(uniqueLogin("alice"));
        ItemBody item = createItem(alice.token(), "poprawna");

        assertEquals(
                HttpStatus.BAD_REQUEST,
                patch(alice.token(), item.id(), Map.of("title", "bez wersji")).getStatusCode()
        );
        assertEquals(HttpStatus.BAD_REQUEST, create(alice.token(), Map.of(
                "content", "bez tytułu")).getStatusCode()
        );
        assertEquals(
                HttpStatus.BAD_REQUEST,
                restTemplate.exchange(
                        "/items/" + item.id() + "/share",
                        HttpMethod.POST,
                        new HttpEntity<>(Map.of("role", "VIEWER"), authHeaders(alice.token())),
                        String.class
                ).getStatusCode()
        );
    }

    @Test
    void whenItemDoesNotExistThenNotFoundIsReturned() {
        Session alice = registerAndLogin(uniqueLogin("alice"));
        UUID missing = UUID.randomUUID();

        assertEquals(HttpStatus.NOT_FOUND, patch(alice.token(), missing, Map.of(
                "title", "x", "version", 0)).getStatusCode()
        );
        assertEquals(HttpStatus.NOT_FOUND, delete(alice.token(), missing).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, historyStatus(alice.token(), missing));
    }

    @RepeatedTest(20)
    void whenTwoPatchesUseSameVersionThenExactlyOneSucceeds() throws Exception {
        Session alice = registerAndLogin(uniqueLogin("alice"));
        ItemBody item = createItem(alice.token(), "pierwsza");

        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            try {
                Future<HttpStatusCode> first = pool.submit(() -> {
                    start.await();
                    return patch(
                            alice.token(), item.id(), Map.of("title", "A", "version", 0)
                    ).getStatusCode();
                });
                Future<HttpStatusCode> second = pool.submit(() -> {
                    start.await();
                    return patch(
                            alice.token(), item.id(), Map.of("title", "B", "version", 0)
                    ).getStatusCode();
                });
                start.countDown();
                List<HttpStatusCode> statuses = List.of(
                        first.get(10, TimeUnit.SECONDS),
                        second.get(10, TimeUnit.SECONDS)
                );
                assertEquals(1, statuses.stream().filter(HttpStatus.OK::equals).count());
                assertEquals(1, statuses.stream().filter(HttpStatus.CONFLICT::equals).count());
            } finally {
                pool.shutdownNow();
            }
        }
        assertEquals(1, findInList(alice.token(), item.id()).version());
    }

    @RepeatedTest(5)
    void whenTwoFirstSharesRaceThenOneIsCreatedAndOtherIsOkOrConflict() throws Exception {
        Session alice = registerAndLogin(uniqueLogin("alice"));
        Session bob = registerAndLogin(uniqueLogin("bob"));
        ItemBody item = createItem(alice.token(), "wspólna");

        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            try {
                Future<HttpStatusCode> first = pool.submit(() -> {
                    start.await();
                    return share(alice.token(), item.id(), bob.id(), "VIEWER").getStatusCode();
                });
                Future<HttpStatusCode> second = pool.submit(() -> {
                    start.await();
                    return share(alice.token(), item.id(), bob.id(), "VIEWER").getStatusCode();
                });
                start.countDown();
                List<HttpStatusCode> statuses = List.of(
                        first.get(10, TimeUnit.SECONDS),
                        second.get(10, TimeUnit.SECONDS)
                );
                assertEquals(
                        1,
                        statuses.stream().filter(HttpStatus.CREATED::equals).count(),
                        () -> "unexpected share race outcome: " + statuses
                );
                assertEquals(
                        1,
                        statuses.stream()
                                .filter(status -> HttpStatus.OK.equals(status)
                                        || HttpStatus.CONFLICT.equals(status))
                                .count(),
                        () -> "unexpected share race outcome: " + statuses
                );
            } finally {
                pool.shutdownNow();
            }
        }
    }

    private Session registerAndLogin(String login) {
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

    private ItemBody createItem(String token, String title) {
        ResponseEntity<ItemBody> response = create(
                token,
                Map.of("title", title, "content", "treść")
        );
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        return response.getBody();
    }

    private ResponseEntity<ItemBody> create(String token, Map<String, Object> body) {
        return restTemplate.exchange(
                "/items",
                HttpMethod.POST,
                new HttpEntity<>(body, authHeaders(token)),
                ItemBody.class
        );
    }

    private ResponseEntity<ItemBody> patch(String token, UUID itemId, Map<String, Object> body) {
        return restTemplate.exchange(
                "/items/" + itemId,
                HttpMethod.PATCH,
                new HttpEntity<>(body, authHeaders(token)),
                ItemBody.class
        );
    }

    private ResponseEntity<Void> delete(String token, UUID itemId) {
        return restTemplate.exchange(
                "/items/" + itemId,
                HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(token)),
                Void.class
        );
    }

    private ResponseEntity<HistoryBody[]> history(String token, UUID itemId) {
        return restTemplate.exchange(
                "/items/" + itemId + "/history",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                HistoryBody[].class
        );
    }

    private HttpStatusCode historyStatus(String token, UUID itemId) {
        return restTemplate.exchange(
                "/items/" + itemId + "/history",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                String.class
        ).getStatusCode();
    }

    private ResponseEntity<ShareBody> share(String token, UUID itemId, UUID userId, String role) {
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

    private ResponseEntity<Void> revoke(String token, UUID itemId, UUID userId) {
        return restTemplate.exchange(
                "/items/" + itemId + "/share/" + userId,
                HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(token)),
                Void.class
        );
    }

    private boolean listContains(String token, UUID itemId) {
        ResponseEntity<ItemSummaryBody[]> response = restTemplate.exchange(
                "/items",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                ItemSummaryBody[].class
        );
        assertNotNull(response.getBody());
        return Arrays.stream(response.getBody()).anyMatch(summary -> summary.id().equals(itemId));
    }

    private ItemSummaryBody findInList(String token, UUID itemId) {
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
                .orElseThrow();
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return headers;
    }

    private static String uniqueLogin(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private record Session(String token, UUID id, String login) {
    }

    private record RegisterBody(UUID id, String login, String createdAt) {
    }

    private record TokenBody(String token, long expiresIn) {
    }

    private record ItemBody(
            UUID id,
            String title,
            String content,
            int version,
            UUID ownerId,
            String createdAt,
            String updatedAt
    ) {
    }

    private record ItemSummaryBody(
            UUID id,
            String title,
            String content,
            int version,
            UUID ownerId,
            String myRole,
            String updatedAt
    ) {
    }

    private record HistoryBody(
            int revision,
            String revisionType,
            String timestamp,
            String changedBy,
            String title,
            String content
    ) {
    }

    private record ShareBody(UUID itemId, UUID userId, String role, String grantedAt) {
    }

    private record ProblemBody(int status, String detail, Integer version) {
    }
}
