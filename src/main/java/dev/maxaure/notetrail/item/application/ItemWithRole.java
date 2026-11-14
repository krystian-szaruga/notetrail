package dev.maxaure.notetrail.item.application;

import dev.maxaure.notetrail.item.domain.Item;
import dev.maxaure.notetrail.item.domain.ItemRole;

public record ItemWithRole(Item item, ItemRole role) {
}
