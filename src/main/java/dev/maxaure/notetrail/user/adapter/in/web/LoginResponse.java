package dev.maxaure.notetrail.user.adapter.in.web;

import dev.maxaure.notetrail.user.application.IssuedToken;

record LoginResponse(String token, long expiresIn) {

    static LoginResponse from(IssuedToken issuedToken) {
        return new LoginResponse(issuedToken.token(), issuedToken.expiresIn());
    }
}
