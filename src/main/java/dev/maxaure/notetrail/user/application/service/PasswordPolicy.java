package dev.maxaure.notetrail.user.application.service;

import java.nio.charset.StandardCharsets;

final class PasswordPolicy {

    static final int MIN_LENGTH = 8;
    static final int MAX_BYTES = 72;

    private PasswordPolicy() {}

    static boolean isTooShort(String raw) {
        return raw.codePointCount(0, raw.length()) < MIN_LENGTH;
    }

    static boolean isTooLong(String raw) {
        return raw.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES;
    }
}