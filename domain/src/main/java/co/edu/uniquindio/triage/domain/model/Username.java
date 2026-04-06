package co.edu.uniquindio.triage.domain.model;

public record Username(String value) {

    private static final int MIN_LENGTH = 3;
    private static final int MAX_LENGTH = 50;

    public Username {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("El username no puede ser null o vacío");
        }

        var trimmed = value.trim();
        if (trimmed.length() < MIN_LENGTH) {
            throw new IllegalArgumentException("El username debe tener al menos 3 caracteres");
        }
        if (trimmed.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("El username no puede tener más de 50 caracteres");
        }

        value = trimmed;
    }

    public static Username of(String value) {
        return new Username(value);
    }
}
