package dev.maxaure.notetrail.item.adapter.in.web;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

record UpdateItemRequest(
        @Size(max = 255) String title,
        String content,
        @NotNull Integer version
) {
}
