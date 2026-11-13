package dev.maxaure.notetrail.user.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.maxaure.notetrail.shared.error.LoginAlreadyTakenException;
import dev.maxaure.notetrail.shared.web.GlobalExceptionHandler;
import dev.maxaure.notetrail.user.application.port.in.RegisterUserUseCase;
import dev.maxaure.notetrail.user.domain.PasswordHash;
import dev.maxaure.notetrail.user.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerTest {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    private final RegisterUserUseCase registerUser = mock(RegisterUserUseCase.class);
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new UserController(registerUser))
            .setControllerAdvice(new GlobalExceptionHandler())
            .setMessageConverters(new MappingJackson2HttpMessageConverter(OBJECT_MAPPER))
            .build();

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
}
