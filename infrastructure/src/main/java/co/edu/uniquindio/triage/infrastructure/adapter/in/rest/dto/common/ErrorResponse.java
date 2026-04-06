package co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.common;

import java.time.OffsetDateTime;
import java.util.List;

public record ErrorResponse(
        int status,
        String error,
        String message,
        OffsetDateTime timestamp,
        List<FieldErrorResponse> fieldErrors
) {
}
