package dev.maxaure.notetrail.item.application;

import dev.maxaure.notetrail.item.domain.ItemPermission;

public record ShareOutcome(ItemPermission permission, boolean created) {
}
