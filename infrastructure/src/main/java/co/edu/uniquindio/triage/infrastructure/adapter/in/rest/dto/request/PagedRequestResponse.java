package co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.request;

import java.util.List;

public record PagedRequestResponse(
        List<RequestResponse> content,
        long totalElements,
        int totalPages,
        int currentPage,
        int pageSize
) {
}
