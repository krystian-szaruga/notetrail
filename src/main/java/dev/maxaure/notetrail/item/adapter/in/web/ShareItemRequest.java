package dev.maxaure.notetrail.item.adapter.in.web;

import dev.maxaure.notetrail.item.domain.ItemRole;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

record ShareItemRequest(
        @NotNull UUID userId,
        @NotNull ItemRole role
) {
}
