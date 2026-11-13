package dev.maxaure.notetrail.user.application;

public record CredentialsCommand(String login, String rawPassword) {

    @Override
    @SuppressWarnings("NullableProblems")
    public String toString() {
        return "CredentialsCommand[login=%s, rawPassword=***]".formatted(login);
    }
}
