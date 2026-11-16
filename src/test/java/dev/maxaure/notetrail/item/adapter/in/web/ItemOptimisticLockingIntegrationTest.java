package dev.maxaure.notetrail.item.adapter.in.web;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class ItemOptimisticLockingIntegrationTest extends ItemApiTestSupport {

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
    void whenEditorUsesStaleVersionThenConflictContainsCurrentVersion() {
        Session alice = registerAndLogin(uniqueLogin("alice"));
        Session bob = registerAndLogin(uniqueLogin("bob"));
        ItemBody item = createItem(alice.token(), "wspólna");
        share(alice.token(), item.id(), bob.id(), "EDITOR");
        patch(alice.token(), item.id(), Map.of("title", "alice", "version", 0));

        ResponseEntity<ProblemBody> response = restTemplate.exchange(
                "/items/" + item.id(),
                HttpMethod.PATCH,
                new HttpEntity<>(Map.of("title", "bob", "version", 0), authHeaders(bob.token())),
                ProblemBody.class
        );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().version());
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
}
