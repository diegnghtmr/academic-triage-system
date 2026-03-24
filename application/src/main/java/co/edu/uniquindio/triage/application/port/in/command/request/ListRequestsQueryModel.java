package co.edu.uniquindio.triage.application.port.in.command.request;

import co.edu.uniquindio.triage.domain.enums.Priority;
import co.edu.uniquindio.triage.domain.enums.RequestStatus;
import co.edu.uniquindio.triage.domain.model.id.RequestTypeId;
import co.edu.uniquindio.triage.domain.model.id.UserId;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

public record ListRequestsQueryModel(
        Optional<RequestStatus> status,
        Optional<RequestTypeId> requestTypeId,
        Optional<Priority> priority,
        Optional<UserId> assignedToUserId,
        Optional<UserId> requesterUserId,
        Optional<LocalDate> dateFrom,
        Optional<LocalDate> dateTo,
        int page,
        int size,
        String sort
) {

    private static final int MAX_PAGE_SIZE = 100;
    private static final String DEFAULT_SORT = "registrationDateTime,desc";

    public ListRequestsQueryModel {
        status = status == null ? Optional.empty() : status;
        requestTypeId = requestTypeId == null ? Optional.empty() : requestTypeId;
        priority = priority == null ? Optional.empty() : priority;
        assignedToUserId = assignedToUserId == null ? Optional.empty() : assignedToUserId;
        requesterUserId = requesterUserId == null ? Optional.empty() : requesterUserId;
        dateFrom = dateFrom == null ? Optional.empty() : dateFrom;
        dateTo = dateTo == null ? Optional.empty() : dateTo;

        if (page < 0) {
            throw new IllegalArgumentException("La página no puede ser negativa");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("El tamaño de página debe estar entre 1 y 100");
        }

        sort = normalizeSort(sort);

        if (dateFrom.isPresent() && dateTo.isPresent() && dateFrom.get().isAfter(dateTo.get())) {
            throw new IllegalArgumentException("dateFrom no puede ser posterior a dateTo");
        }
    }

    private static String normalizeSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return DEFAULT_SORT;
        }
        var normalized = sort.trim();
        if (!normalized.contains(",")) {
            throw new IllegalArgumentException("El sort debe incluir campo y dirección separados por coma");
        }
        return normalized;
    }
}
