package dev.maxaure.notetrail.user.application;

public record IssuedToken(String token, long expiresIn) {

    @Override
    @SuppressWarnings("NullableProblems")
    public String toString() {
        return "IssuedToken[token=***, expiresIn=%s]".formatted(expiresIn);
    }
}
