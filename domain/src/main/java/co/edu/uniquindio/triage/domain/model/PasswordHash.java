package co.edu.uniquindio.triage.domain.model;

public record PasswordHash(String value) {

    private static final int MAX_LENGTH = 255;

    public PasswordHash {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("El password hash no puede ser null o vacío");
        }

        var trimmed = value.trim();
        if (trimmed.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("El password hash no puede tener más de 255 caracteres");
        }

        value = trimmed;
    }

    public static PasswordHash of(String value) {
        return new PasswordHash(value);
    }
}
