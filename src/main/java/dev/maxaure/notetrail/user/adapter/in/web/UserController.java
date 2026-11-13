package dev.maxaure.notetrail.user.adapter.in.web;

import dev.maxaure.notetrail.user.application.CredentialsCommand;
import dev.maxaure.notetrail.user.application.IssuedToken;
import dev.maxaure.notetrail.user.application.port.in.LoginUserUseCase;
import dev.maxaure.notetrail.user.application.port.in.RegisterUserUseCase;
import dev.maxaure.notetrail.user.domain.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
class UserController {

    private final RegisterUserUseCase registerUser;
    private final LoginUserUseCase loginUser;

    UserController(RegisterUserUseCase registerUser, LoginUserUseCase loginUser) {
        this.registerUser = registerUser;
        this.loginUser = loginUser;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    RegisterResponse register(@Valid @RequestBody CredentialsRequest request) {
        User user = registerUser.register(new CredentialsCommand(request.login(), request.password()));
        return RegisterResponse.from(user);
    }

    @PostMapping("/login")
    LoginResponse login(@Valid @RequestBody CredentialsRequest request) {
        IssuedToken issuedToken = loginUser.login(new CredentialsCommand(request.login(), request.password()));
        return LoginResponse.from(issuedToken);
    }
}
