package dev.maxaure.notetrail.item.adapter.in.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ItemAuthorizationIntegrationTest extends ItemApiTestSupport {

    // ---------------------------------------------------------------
    // 401: missing token
    // ---------------------------------------------------------------

    @Test
    void whenRequestHasNoTokenThenUnauthorizedIsReturned() {
        assertEquals(
                HttpStatus.UNAUTHORIZED,
                restTemplate.getForEntity("/items", String.class).getStatusCode()
        );
    }

    @Test
    void whenCreateRequestHasNoTokenThenUnauthorizedIsReturned() {
        assertEquals(
                HttpStatus.UNAUTHORIZED,
                withoutToken(HttpMethod.POST, "/items", Map.of("title", "x")).getStatusCode()
        );
    }

    @Test
    void whenPatchRequestHasNoTokenThenUnauthorizedIsReturned() {
        Session alice = registerAndLogin(uniqueLogin("alice"));
        ItemBody item = createItem(alice.token(), "x");

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                withoutToken(HttpMethod.PATCH, "/items/" + item.id(),
                        Map.of("title", "y", "version", 0)).getStatusCode()
        );
    }

    @Test
    void whenHistoryRequestHasNoTokenThenUnauthorizedIsReturned() {
        Session alice = registerAndLogin(uniqueLogin("alice"));
        ItemBody item = createItem(alice.token(), "x");

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                withoutToken(HttpMethod.GET, "/items/" + item.id() + "/history", null)
                        .getStatusCode()
        );
    }

    @Test
    void whenDeleteRequestHasNoTokenThenUnauthorizedIsReturned() {
        Session alice = registerAndLogin(uniqueLogin("alice"));
        ItemBody item = createItem(alice.token(), "x");

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                withoutToken(HttpMethod.DELETE, "/items/" + item.id(), null).getStatusCode()
        );
    }

    @Test
    void whenShareRequestHasNoTokenThenUnauthorizedIsReturned() {
        Session alice = registerAndLogin(uniqueLogin("alice"));
        Session bob = registerAndLogin(uniqueLogin("bob"));
        ItemBody item = createItem(alice.token(), "x");

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                withoutToken(HttpMethod.POST, "/items/" + item.id() + "/share",
                        Map.of("userId", bob.id().toString(), "role", "VIEWER")).getStatusCode()
        );
    }

    @Test
    void whenRevokeRequestHasNoTokenThenUnauthorizedIsReturned() {
        Session alice = registerAndLogin(uniqueLogin("alice"));
        Session bob = registerAndLogin(uniqueLogin("bob"));
        ItemBody item = createItem(alice.token(), "x");
        share(alice.token(), item.id(), bob.id(), "VIEWER");

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                withoutToken(HttpMethod.DELETE,
                        "/items/" + item.id() + "/share/" + bob.id(), null).getStatusCode()
        );
        assertTrue(listContains(bob.token(), item.id()));
    }

    // ---------------------------------------------------------------
    // 403: roles per resource
    // ---------------------------------------------------------------

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
    void whenEditorDeletesItemThenForbiddenIsReturnedAndItemSurvives() {
        Session alice = registerAndLogin(uniqueLogin("alice"));
        Session bob = registerAndLogin(uniqueLogin("bob"));
        ItemBody item = createItem(alice.token(), "wspólna");
        share(alice.token(), item.id(), bob.id(), "EDITOR");

        assertEquals(HttpStatus.FORBIDDEN, delete(bob.token(), item.id()).getStatusCode());
        assertTrue(listContains(alice.token(), item.id()));
    }
}
