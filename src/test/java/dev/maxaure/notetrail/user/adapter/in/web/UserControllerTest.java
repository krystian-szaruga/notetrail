package dev.maxaure.notetrail.user.adapter.in.web;

import dev.maxaure.notetrail.user.domain.error.LoginAlreadyTakenException;
import dev.maxaure.notetrail.core.error.UnauthorizedException;
import dev.maxaure.notetrail.user.application.IssuedToken;
import dev.maxaure.notetrail.user.application.port.in.LoginUserUseCase;
import dev.maxaure.notetrail.user.application.port.in.RegisterUserUseCase;
import dev.maxaure.notetrail.user.domain.PasswordHash;
import dev.maxaure.notetrail.user.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegisterUserUseCase registerUser;

    @MockitoBean
    private LoginUserUseCase loginUser;

    @Test
    void whenUserIsRegisteredThenResponseContainsNoPasswordHash() throws Exception {
        UUID id = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        Instant createdAt = Instant.parse("2026-01-01T10:00:00Z");
        User user = new User(id, "jan.kowalski", new PasswordHash("$2a$10$hash"), createdAt);

        when(registerUser.register(any()))
                .thenReturn(user);

        mockMvc.perform(
                post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"jan.kowalski","password":"strong-pass"}
                                """
                        )
        )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.login").value("jan.kowalski"))
                .andExpect(jsonPath("$.createdAt").value("2026-01-01T10:00:00Z"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void whenLoginIsAlreadyTakenThenClientGetsConflict() throws Exception {
        when(registerUser.register(any()))
                .thenThrow(new LoginAlreadyTakenException("jan.kowalski"));

        mockMvc.perform(
                post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"jan.kowalski","password":"strong-pass"}
                                """
                        )
        )
                .andExpect(status().isConflict());
    }

    @Test
    void whenRequestMissesPasswordThenClientGetsBadRequest() throws Exception {
        mockMvc.perform(
                post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"jan.kowalski"}
                                """
                        )
        )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(registerUser);
    }

    @Test
    void whenCredentialsAreValidThenClientGetsToken() throws Exception {
        when(loginUser.login(any()))
                .thenReturn(new IssuedToken("jwt-token", 3600));

        mockMvc.perform(
                post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"jan.kowalski","password":"strong-pass"}
                                """
                        )
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.expiresIn").value(3600));
    }

    @Test
    void whenCredentialsAreInvalidThenClientGetsUnauthorized() throws Exception {
        when(loginUser.login(any()))
                .thenThrow(new UnauthorizedException("invalid login or password"));

        mockMvc.perform(
                post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"jan.kowalski","password":"wrong-pass"}
                                """
                        )
        )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void whenLoginRequestMissesPasswordThenClientGetsBadRequest() throws Exception {
        mockMvc.perform(
                post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"jan.kowalski"}
                                """
                        )
        )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(loginUser);
    }
}
