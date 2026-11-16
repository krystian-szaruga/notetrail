package dev.maxaure.notetrail.item.adapter.in.web;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class ItemSharingIntegrationTest extends ItemApiTestSupport {

    // ---------------------------------------------------------------
    // POST /items/{id}/share
    // ---------------------------------------------------------------

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
    void whenShareRoleIsInvalidOrMissingThenBadRequestIsReturned() {
        Session alice = registerAndLogin(uniqueLogin("alice"));
        Session bob = registerAndLogin(uniqueLogin("bob"));
        ItemBody item = createItem(alice.token(), "wspólna");

        assertEquals(HttpStatus.BAD_REQUEST,
                share(alice.token(), item.id(), bob.id(), "ADMIN").getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, restTemplate.exchange(
                "/items/" + item.id() + "/share",
                HttpMethod.POST,
                new HttpEntity<>(Map.of("userId", bob.id().toString()), authHeaders(alice.token())),
                String.class
        ).getStatusCode());
        assertFalse(listContains(bob.token(), item.id()));
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
    void whenSharedItemDoesNotExistThenNotFoundIsReturned() {
        Session alice = registerAndLogin(uniqueLogin("alice"));
        Session bob = registerAndLogin(uniqueLogin("bob"));

        assertEquals(HttpStatus.NOT_FOUND,
                share(alice.token(), UUID.randomUUID(), bob.id(), "VIEWER").getStatusCode());
    }

    @RepeatedTest(20)
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

    // ---------------------------------------------------------------
    // DELETE /items/{id}/share/{userId}
    // ---------------------------------------------------------------

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
    void whenRevokedItemDoesNotExistThenNotFoundIsReturned() {
        Session alice = registerAndLogin(uniqueLogin("alice"));
        Session bob = registerAndLogin(uniqueLogin("bob"));

        assertEquals(HttpStatus.NOT_FOUND,
                revoke(alice.token(), UUID.randomUUID(), bob.id()).getStatusCode());
    }
}