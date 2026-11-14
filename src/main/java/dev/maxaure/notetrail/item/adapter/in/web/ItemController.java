package dev.maxaure.notetrail.item.adapter.in.web;

import dev.maxaure.notetrail.item.application.CreateItemCommand;
import dev.maxaure.notetrail.item.application.ShareItemCommand;
import dev.maxaure.notetrail.item.application.ShareOutcome;
import dev.maxaure.notetrail.item.application.UpdateItemCommand;
import dev.maxaure.notetrail.item.application.port.in.CreateItemUseCase;
import dev.maxaure.notetrail.item.application.port.in.DeleteItemUseCase;
import dev.maxaure.notetrail.item.application.port.in.GetItemHistoryUseCase;
import dev.maxaure.notetrail.item.application.port.in.ListItemsUseCase;
import dev.maxaure.notetrail.item.application.port.in.RevokeShareUseCase;
import dev.maxaure.notetrail.item.application.port.in.ShareItemUseCase;
import dev.maxaure.notetrail.item.application.port.in.UpdateItemUseCase;
import dev.maxaure.notetrail.item.domain.Item;
import dev.maxaure.notetrail.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/items")
class ItemController {

    private final CreateItemUseCase createItem;
    private final ListItemsUseCase listItems;
    private final UpdateItemUseCase updateItem;
    private final DeleteItemUseCase deleteItem;
    private final GetItemHistoryUseCase getItemHistory;
    private final ShareItemUseCase shareItem;
    private final RevokeShareUseCase revokeShare;

    ItemController(
            CreateItemUseCase createItem,
            ListItemsUseCase listItems,
            UpdateItemUseCase updateItem,
            DeleteItemUseCase deleteItem,
            GetItemHistoryUseCase getItemHistory,
            ShareItemUseCase shareItem,
            RevokeShareUseCase revokeShare
    ) {
        this.createItem = createItem;
        this.listItems = listItems;
        this.updateItem = updateItem;
        this.deleteItem = deleteItem;
        this.getItemHistory = getItemHistory;
        this.shareItem = shareItem;
        this.revokeShare = revokeShare;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ItemResponse create(@Valid @RequestBody CreateItemRequest request, AuthenticatedUser user) {
        Item item = createItem.create(new CreateItemCommand(
                user.id(),
                request.title(),
                request.content()
        ));
        return ItemResponse.from(item);
    }

    @GetMapping
    List<ItemSummaryResponse> list(AuthenticatedUser user) {
        return listItems.list(user.id()).stream()
                .map(ItemSummaryResponse::from)
                .toList();
    }

    @PatchMapping("/{id}")
    ItemResponse update(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateItemRequest request,
            AuthenticatedUser user
    ) {
        Item item = updateItem.update(new UpdateItemCommand(
                id,
                user.id(),
                request.title(),
                request.content(),
                request.version()
        ));
        return ItemResponse.from(item);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable("id") UUID id, AuthenticatedUser user) {
        deleteItem.delete(id, user.id());
    }

    @GetMapping("/{id}/history")
    List<ItemHistoryResponse> history(@PathVariable("id") UUID id, AuthenticatedUser user) {
        return getItemHistory.history(id, user.id()).stream()
                .map(ItemHistoryResponse::from)
                .toList();
    }

    @PostMapping("/{id}/share")
    ResponseEntity<ShareResponse> share(
            @PathVariable("id") UUID id,
            @Valid @RequestBody ShareItemRequest request,
            AuthenticatedUser user
    ) {
        ShareOutcome outcome = shareItem.share(new ShareItemCommand(
                id,
                user.id(),
                request.userId(),
                request.role()
        ));
        HttpStatus status = outcome.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(ShareResponse.from(outcome.permission()));
    }

    @DeleteMapping("/{id}/share/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void revoke(
            @PathVariable("id") UUID id,
            @PathVariable("userId") UUID userId,
            AuthenticatedUser user
    ) {
        revokeShare.revoke(id, user.id(), userId);
    }
}
