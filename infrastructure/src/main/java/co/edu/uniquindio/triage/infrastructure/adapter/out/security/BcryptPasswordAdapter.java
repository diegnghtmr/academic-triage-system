package co.edu.uniquindio.triage.infrastructure.adapter.out.security;

import co.edu.uniquindio.triage.application.port.out.security.PasswordEncoderPort;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Objects;

public class BcryptPasswordAdapter implements PasswordEncoderPort {

    private final PasswordEncoder passwordEncoder;

    public BcryptPasswordAdapter(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder);
    }

    @Override
    public String encode(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
}
