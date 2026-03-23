package co.edu.uniquindio.triage.infrastructure.adapter.in.rest;

import co.edu.uniquindio.triage.application.port.in.auth.LoginUseCase;
import co.edu.uniquindio.triage.application.port.in.auth.RegisterUseCase;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.auth.AuthResponse;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.auth.LoginRequest;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.auth.RegisterRequest;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.user.UserResponse;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.mapper.AuthRestMapper;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.support.AuthenticatedActorMapper;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.mapper.UserRestMapper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final RegisterUseCase registerUseCase;
    private final LoginUseCase loginUseCase;
    private final AuthRestMapper authRestMapper;
    private final UserRestMapper userRestMapper;
    private final AuthenticatedActorMapper authenticatedActorMapper;

    public AuthController(RegisterUseCase registerUseCase,
                          LoginUseCase loginUseCase,
                          AuthRestMapper authRestMapper,
                          UserRestMapper userRestMapper,
                          AuthenticatedActorMapper authenticatedActorMapper) {
        this.registerUseCase = Objects.requireNonNull(registerUseCase);
        this.loginUseCase = Objects.requireNonNull(loginUseCase);
        this.authRestMapper = Objects.requireNonNull(authRestMapper);
        this.userRestMapper = Objects.requireNonNull(userRestMapper);
        this.authenticatedActorMapper = Objects.requireNonNull(authenticatedActorMapper);
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request,
                                                 Authentication authentication) {
        var actor = authenticatedActorMapper.toOptionalActor(authentication);
        var user = registerUseCase.register(authRestMapper.toCommand(request), actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(userRestMapper.toResponse(user));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        var result = loginUseCase.login(authRestMapper.toCommand(request));
        return ResponseEntity.ok(authRestMapper.toResponse(result));
    }
}
