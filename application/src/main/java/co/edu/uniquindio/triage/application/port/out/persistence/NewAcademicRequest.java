package co.edu.uniquindio.triage.application.port.out.persistence;

import co.edu.uniquindio.triage.domain.model.AcademicRequest;
import co.edu.uniquindio.triage.domain.model.id.OriginChannelId;
import co.edu.uniquindio.triage.domain.model.id.RequestTypeId;
import co.edu.uniquindio.triage.domain.model.id.UserId;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

public record NewAcademicRequest(
        String description,
        UserId applicantId,
        OriginChannelId originChannelId,
        RequestTypeId requestTypeId,
        LocalDate deadline,
        boolean aiSuggested,
        LocalDateTime registrationDateTime
) {

    public NewAcademicRequest {
        description = AcademicRequest.normalizeDescription(description);
        Objects.requireNonNull(applicantId, "El applicantId no puede ser null");
        Objects.requireNonNull(originChannelId, "El originChannelId no puede ser null");
        Objects.requireNonNull(requestTypeId, "El requestTypeId no puede ser null");
        Objects.requireNonNull(registrationDateTime, "El registrationDateTime no puede ser null");
    }
}
