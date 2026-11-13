package dev.maxaure.notetrail.user.adapter.in.web;

import dev.maxaure.notetrail.user.application.RegisterUserCommand;
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

    UserController(RegisterUserUseCase registerUser) {
        this.registerUser = registerUser;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        User user = registerUser.register(new RegisterUserCommand(request.login(), request.password()));
        return RegisterResponse.from(user);
    }
}
