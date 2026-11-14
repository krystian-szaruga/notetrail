package dev.maxaure.notetrail.item.adapter.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record CreateItemRequest(
        @NotBlank @Size(max = 255) String title,
        String content
) {
}
