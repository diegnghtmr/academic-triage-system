package co.edu.uniquindio.triage.application.port.in.user.command;

import co.edu.uniquindio.triage.domain.enums.Role;

import java.util.Optional;

public record GetUsersQueryModel(
        Optional<Role> role,
        Optional<Boolean> active,
        int page,
        int size,
        String sort
) {
    private static final int MAX_PAGE_SIZE = 100;
    private static final String DEFAULT_SORT = "username,asc";

    public GetUsersQueryModel {
        role = role == null ? Optional.empty() : role;
        active = active == null ? Optional.empty() : active;
        
        if (page < 0) {
            throw new IllegalArgumentException("La página no puede ser negativa");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("El tamaño de página debe estar entre 1 y 100");
        }
        sort = (sort == null || sort.isBlank()) ? DEFAULT_SORT : sort;
    }
}
