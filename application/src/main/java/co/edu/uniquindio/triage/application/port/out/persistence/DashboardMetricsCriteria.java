package co.edu.uniquindio.triage.application.port.out.persistence;

import java.time.LocalDateTime;
import java.util.Optional;

public record DashboardMetricsCriteria(
    Optional<LocalDateTime> from,
    Optional<LocalDateTime> toExclusive
) {
}
