package co.edu.uniquindio.triage.application.port.in.command.report;

import java.time.LocalDate;
import java.util.Optional;

public record GetDashboardMetricsQueryModel(
    Optional<LocalDate> dateFrom,
    Optional<LocalDate> dateTo
) {
    public GetDashboardMetricsQueryModel {
        dateFrom = dateFrom == null ? Optional.empty() : dateFrom;
        dateTo = dateTo == null ? Optional.empty() : dateTo;

        if (dateFrom.isPresent() && dateTo.isPresent() && dateFrom.get().isAfter(dateTo.get())) {
            throw new IllegalArgumentException("dateFrom no puede ser posterior a dateTo");
        }
    }
}
