package dev.maxaure.notetrail.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.maxaure.notetrail.core.error.UnauthorizedException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

class JwtAuthenticationFilter extends OncePerRequestFilter {

    static final String USER_ATTRIBUTE = AuthenticatedUser.class.getName();

    private static final Set<String> PUBLIC_PATHS = Set.of("/register", "/login");

    private final JwtTokenVerifier verifier;
    private final ObjectMapper objectMapper;

    JwtAuthenticationFilter(JwtTokenVerifier verifier, ObjectMapper objectMapper) {
        this.verifier = verifier;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return PUBLIC_PATHS.contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain chain
    ) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            writeUnauthorized(response, "missing or malformed Authorization header");
            return;
        }
        String token = header.substring("Bearer ".length());
        try {
            AuthenticatedUser user = verifier.verify(token);
            request.setAttribute(USER_ATTRIBUTE, user);
            try (var ignored = CurrentUser.open(user)) {
                chain.doFilter(request, response);
            }
        } catch (UnauthorizedException e) {
            writeUnauthorized(response, e.getMessage());
        }
    }

    private void writeUnauthorized(HttpServletResponse response, String detail) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, detail);
        problem.setTitle(HttpStatus.UNAUTHORIZED.getReasonPhrase());
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), problem);
    }
}
