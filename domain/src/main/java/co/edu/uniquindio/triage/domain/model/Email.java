package co.edu.uniquindio.triage.domain.model;

import java.util.regex.Pattern;

public record Email(String value) {
    private static final int MAX_LENGTH = 255;
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[\\w.-]+@[\\w.-]+\\.\\w{2,}$"
    );

    public Email {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("El email no puede ser null o vacío");
        }
        String trimmed = value.trim();
        if (trimmed.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("El email no puede tener más de 255 caracteres");
        }
        if (!EMAIL_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("El formato del email es inválido: " + trimmed);
        }
        value = trimmed;
    }

    public static Email of(String value) {
        return new Email(value);
    }
}
