package co.edu.uniquindio.triage.application.port.in.common;

import java.util.List;

public record Page<T>(
        List<T> content,
        long totalElements,
        int totalPages,
        int currentPage,
        int pageSize
) {

    public Page {
        content = content == null ? List.of() : List.copyOf(content);
        if (totalElements < 0) {
            throw new IllegalArgumentException("El total de elementos no puede ser negativo");
        }
        if (totalPages < 0) {
            throw new IllegalArgumentException("El total de páginas no puede ser negativo");
        }
        if (currentPage < 0) {
            throw new IllegalArgumentException("La página actual no puede ser negativa");
        }
        if (pageSize < 1) {
            throw new IllegalArgumentException("El tamaño de página debe ser positivo");
        }
    }
}
