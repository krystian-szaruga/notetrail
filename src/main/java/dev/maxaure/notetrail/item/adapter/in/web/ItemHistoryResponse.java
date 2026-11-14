package dev.maxaure.notetrail.item.adapter.in.web;

import dev.maxaure.notetrail.item.domain.ItemRevision;
import dev.maxaure.notetrail.item.domain.RevisionType;

import java.time.Instant;

record ItemHistoryResponse(
        int revision,
        RevisionType revisionType,
        Instant timestamp,
        String changedBy,
        String title,
        String content
) {

    static ItemHistoryResponse from(ItemRevision revision) {
        return new ItemHistoryResponse(
                revision.revision(),
                revision.type(),
                revision.timestamp(),
                revision.changedBy(),
                revision.title(),
                revision.content()
        );
    }
}
