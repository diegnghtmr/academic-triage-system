package co.edu.uniquindio.triage.domain.model;

import co.edu.uniquindio.triage.domain.model.id.RequestTypeId;

import java.util.Objects;

public class RequestType {
    private final RequestTypeId id;
    private String name;
    private String description;
    private boolean active;

    public RequestType(RequestTypeId id, String name, String description, boolean active) {
        this.id = Objects.requireNonNull(id, "El id no puede ser null");
        this.name = validateName(name);
        this.description = validateDescription(description);
        this.active = active;
    }

    private RequestType(String name, String description) {
        this.id = null;
        this.name = validateName(name);
        this.description = validateDescription(description);
        this.active = true;
    }

    public static RequestType createNew(String name, String description) {
        return new RequestType(name, description);
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

    private String validateDescription(String description) {
        if (description != null && description.length() > 500) {
            throw new IllegalArgumentException("La descripción no puede tener más de 500 caracteres");
        }
        return description != null ? description.trim() : null;
    }

    public RequestTypeId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void updateName(String name) {
        this.name = validateName(name);
    }

    public String getDescription() {
        return description;
    }

    public void updateDescription(String description) {
        this.description = validateDescription(description);
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
        RequestType that = (RequestType) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "RequestType{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", active=" + active +
                '}';
    }
}
