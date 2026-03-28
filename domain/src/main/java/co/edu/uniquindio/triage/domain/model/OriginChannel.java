package co.edu.uniquindio.triage.domain.model;

import co.edu.uniquindio.triage.domain.model.id.OriginChannelId;

import java.util.Objects;

public class OriginChannel {
    private final OriginChannelId id;
    private String name;
    private boolean active;

    public OriginChannel(OriginChannelId id, String name, boolean active) {
        this.id = Objects.requireNonNull(id, "El id no puede ser null");
        this.name = validateName(name);
        this.active = active;
    }

    private OriginChannel(String name) {
        this.id = null;
        this.name = validateName(name);
        this.active = true;
    }

    public static OriginChannel createNew(String name) {
        return new OriginChannel(name);
    }

    private String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre no puede ser null o vacío");
        }
        String trimmed = name.trim();
        if (trimmed.length() > 100) {
            throw new IllegalArgumentException("El nombre no puede tener más de 100 caracteres");
        }
        return trimmed;
    }

    public OriginChannelId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void updateName(String name) {
        this.name = validateName(name);
    }

    public boolean isActive() {
        return active;
    }

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OriginChannel that = (OriginChannel) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "OriginChannel{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", active=" + active +
                '}';
    }
}
