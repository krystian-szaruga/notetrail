package dev.maxaure.notetrail.user.application;

public record RegisterUserCommand(String login, String rawPassword) {

    @Override
    @SuppressWarnings("NullableProblems")
    public String toString() {
        return "RegisterUserCommand[login=%s, rawPassword=***]".formatted(login);
    }
}
