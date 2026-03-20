package co.edu.uniquindio.triage.domain.model.vo;

import java.util.regex.Pattern;

public record UserEmail(String value) {
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[\\w.-]+@[\\w.-]+\\.\\w{2,}$"
    );

    public UserEmail {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("El email no puede ser null o vacío");
        }
        String trimmed = value.trim();
        if (!EMAIL_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("El formato del email es inválido: " + trimmed);
        }
        value = trimmed;
    }

    public static UserEmail of(String value) {
        return new UserEmail(value);
    }
}
