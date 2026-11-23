package dev.maxaure.notetrail.item.adapter.in.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ItemCrudIntegrationTest extends ItemApiTestSupport {

    // ---------------------------------------------------------------
    // POST /items
    // ---------------------------------------------------------------

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
    void whenTitleIsEmptyOrTooLongThenBadRequestIsReturned() {
        Session alice = registerAndLogin(uniqueLogin("alice"));

        assertEquals(HttpStatus.BAD_REQUEST,
                create(alice.token(), Map.of("title", "")).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST,
                create(alice.token(), Map.of("title", "a".repeat(256))).getStatusCode());
        assertEquals(HttpStatus.CREATED,
                create(alice.token(), Map.of("title", "a".repeat(255))).getStatusCode());
    }

    @Test
    void whenContentIsOmittedThenItemIsCreatedWithNullContent() {
        Session alice = registerAndLogin(uniqueLogin("alice"));

        ResponseEntity<ItemBody> response = create(alice.token(), Map.of("title", "bez tresci"));

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNull(response.getBody().content());
        assertEquals(0, response.getBody().version());
    }

    // ---------------------------------------------------------------
    // GET /items
    // ---------------------------------------------------------------

    @Test
    void whenOwnerListsItemsThenMyRoleIsOwner() {
        Session alice = registerAndLogin(uniqueLogin("alice"));
        ItemBody item = createItem(alice.token(), "moja");

        assertEquals("OWNER", findInList(alice.token(), item.id()).myRole());
    }

    // ---------------------------------------------------------------
    // PATCH /items/{id}
    // ---------------------------------------------------------------

    @Test
    void whenOnlyTitleIsPatchedThenContentIsKept() {
        Session alice = registerAndLogin(uniqueLogin("alice"));
        ItemBody item = createItem(alice.token(), "tytul");

        ItemPatchBody body = patch(
                alice.token(), item.id(), Map.of("title", "inny", "version", 0)).getBody();

        assertNotNull(body);
        assertEquals("inny", body.title());
        assertEquals("treść", body.content());
    }

    @Test
    void whenItemIsPatchedThenResponseHasExactlyTheContractFields() throws Exception {
        Session alice = registerAndLogin(uniqueLogin("alice"));
        ItemBody item = createItem(alice.token(), "tytuł");

        String json = restTemplate.exchange(
                "/items/" + item.id(),
                HttpMethod.PATCH,
                new HttpEntity<>(
                        Map.of("title", "inny", "version", 0),
                        authHeaders(alice.token())
                ),
                String.class
        ).getBody();
        assertNotNull(json);

        JsonNode body = new ObjectMapper().readTree(json);
        Set<String> fields = new LinkedHashSet<>();
        body.fieldNames().forEachRemaining(fields::add);

        assertEquals(Set.of("id", "title", "content", "version", "updatedAt"), fields);
    }

    // ---------------------------------------------------------------
    // validation and not found
    // ---------------------------------------------------------------

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

    // ---------------------------------------------------------------
    // soft delete
    // ---------------------------------------------------------------

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
    void whenOwnerSoftDeletesSharedItemThenItDisappearsForSharedUser() {
        Session alice = registerAndLogin(uniqueLogin("alice"));
        Session bob = registerAndLogin(uniqueLogin("bob"));
        ItemBody item = createItem(alice.token(), "wspólna");
        share(alice.token(), item.id(), bob.id(), "VIEWER");
        assertTrue(listContains(bob.token(), item.id()));

        assertEquals(HttpStatus.NO_CONTENT, delete(alice.token(), item.id()).getStatusCode());

        assertFalse(listContains(bob.token(), item.id()));
        assertEquals(HttpStatus.NOT_FOUND, historyStatus(bob.token(), item.id()));
    }
}
