package dev.maxaure.notetrail.item.adapter.in.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ItemHistoryIntegrationTest extends ItemApiTestSupport {

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
}
