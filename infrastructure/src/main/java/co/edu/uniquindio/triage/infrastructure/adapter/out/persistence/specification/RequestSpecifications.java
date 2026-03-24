package co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.specification;

import co.edu.uniquindio.triage.application.port.out.persistence.RequestSearchCriteria;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.entity.AcademicRequestJpaEntity;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public final class RequestSpecifications {

    private static final Specification<AcademicRequestJpaEntity> UNRESTRICTED =
            (root, query, cb) -> cb.conjunction();

    private RequestSpecifications() {
    }

    public static Specification<AcademicRequestJpaEntity> withCriteria(RequestSearchCriteria criteria) {
        return hasStatus(criteria)
                .and(hasRequestType(criteria))
                .and(hasPriority(criteria))
                .and(hasAssignedTo(criteria))
                .and(hasRequester(criteria))
                .and(hasDateFrom(criteria))
                .and(hasDateTo(criteria));
    }

    private static Specification<AcademicRequestJpaEntity> hasStatus(RequestSearchCriteria criteria) {
        return criteria.status()
                .map(status -> (Specification<AcademicRequestJpaEntity>) (root, query, cb) -> cb.equal(root.get("status"), status.name()))
                .orElse(UNRESTRICTED);
    }

    private static Specification<AcademicRequestJpaEntity> hasRequestType(RequestSearchCriteria criteria) {
        return criteria.requestTypeId()
                .map(requestTypeId -> (Specification<AcademicRequestJpaEntity>) (root, query, cb) -> cb.equal(root.get("requestType").get("id"), requestTypeId.value()))
                .orElse(UNRESTRICTED);
    }

    private static Specification<AcademicRequestJpaEntity> hasPriority(RequestSearchCriteria criteria) {
        return criteria.priority()
                .map(priority -> (Specification<AcademicRequestJpaEntity>) (root, query, cb) -> cb.equal(root.get("priority"), priority.name()))
                .orElse(UNRESTRICTED);
    }

    private static Specification<AcademicRequestJpaEntity> hasAssignedTo(RequestSearchCriteria criteria) {
        return criteria.assignedToUserId()
                .map(assignedToUserId -> (Specification<AcademicRequestJpaEntity>) (root, query, cb) -> cb.equal(root.get("responsible").get("id"), assignedToUserId.value()))
                .orElse(UNRESTRICTED);
    }

    private static Specification<AcademicRequestJpaEntity> hasRequester(RequestSearchCriteria criteria) {
        return criteria.requesterUserId()
                .map(requesterUserId -> (Specification<AcademicRequestJpaEntity>) (root, query, cb) -> cb.equal(root.get("applicant").get("id"), requesterUserId.value()))
                .orElse(UNRESTRICTED);
    }

    private static Specification<AcademicRequestJpaEntity> hasDateFrom(RequestSearchCriteria criteria) {
        return criteria.dateFrom()
                .map(dateFrom -> (Specification<AcademicRequestJpaEntity>) (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("registrationDateTime"), dateFrom.atStartOfDay()))
                .orElse(UNRESTRICTED);
    }

    private static Specification<AcademicRequestJpaEntity> hasDateTo(RequestSearchCriteria criteria) {
        return criteria.dateTo()
                .map(dateTo -> {
                    var inclusiveEnd = LocalDateTime.of(dateTo.plusDays(1), java.time.LocalTime.MIN);
                    return (Specification<AcademicRequestJpaEntity>) (root, query, cb) -> cb.lessThan(root.get("registrationDateTime"), inclusiveEnd);
                })
                .orElse(UNRESTRICTED);
    }
}
