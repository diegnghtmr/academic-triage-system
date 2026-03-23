package co.edu.uniquindio.triage.domain.model;

public record Identification(String value) {

    private static final int MAX_LENGTH = 20;

    public Identification {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("La identificación no puede ser null o vacía");
        }

        var trimmed = value.trim();
        if (trimmed.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("La identificación no puede tener más de 20 caracteres");
        }

        value = trimmed;
    }

    public static Identification of(String value) {
        return new Identification(value);
    }
}
