package co.edu.uniquindio.triage.application.port.out.persistence;

import co.edu.uniquindio.triage.domain.enums.Role;

import java.util.Optional;

public record UserSearchCriteria(
        Optional<Role> role,
        Optional<Boolean> active,
        int page,
        int size,
        String sort
) {

    public UserSearchCriteria {
        role = role == null ? Optional.empty() : role;
        active = active == null ? Optional.empty() : active;

        if (page < 0) {
            throw new IllegalArgumentException("La página no puede ser negativa");
        }
        if (size < 1) {
            throw new IllegalArgumentException("El tamaño de página debe ser positivo");
        }
        if (sort == null || sort.isBlank()) {
            throw new IllegalArgumentException("El sort no puede ser null o vacío");
        }
        sort = sort.trim();
    }
}
