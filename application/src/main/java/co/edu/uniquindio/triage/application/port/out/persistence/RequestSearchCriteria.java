package co.edu.uniquindio.triage.application.port.out.persistence;

import co.edu.uniquindio.triage.domain.enums.Priority;
import co.edu.uniquindio.triage.domain.enums.RequestStatus;
import co.edu.uniquindio.triage.domain.model.id.RequestTypeId;
import co.edu.uniquindio.triage.domain.model.id.UserId;

import java.time.LocalDate;
import java.util.Optional;

public record RequestSearchCriteria(
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

    public RequestSearchCriteria {
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
        if (size < 1) {
            throw new IllegalArgumentException("El tamaño de página debe ser positivo");
        }
        if (sort == null || sort.isBlank()) {
            throw new IllegalArgumentException("El sort no puede ser null o vacío");
        }
        sort = sort.trim();
    }
}
