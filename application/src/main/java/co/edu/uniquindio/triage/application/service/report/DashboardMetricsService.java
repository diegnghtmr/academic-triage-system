package co.edu.uniquindio.triage.application.service.report;

import co.edu.uniquindio.triage.application.port.in.auth.AuthenticatedActor;
import co.edu.uniquindio.triage.application.port.in.command.report.GetDashboardMetricsQueryModel;
import co.edu.uniquindio.triage.application.port.in.report.DashboardMetricsView;
import co.edu.uniquindio.triage.application.port.in.report.GetDashboardMetricsQuery;
import co.edu.uniquindio.triage.application.port.out.persistence.DashboardMetricsCriteria;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadDashboardMetricsPort;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

public class DashboardMetricsService implements GetDashboardMetricsQuery {

    private final LoadDashboardMetricsPort loadDashboardMetricsPort;
    private final ReportAuthorizationSupport authSupport;

    public DashboardMetricsService(LoadDashboardMetricsPort loadDashboardMetricsPort, ReportAuthorizationSupport authSupport) {
        this.loadDashboardMetricsPort = Objects.requireNonNull(loadDashboardMetricsPort, "loadDashboardMetricsPort must not be null");
        this.authSupport = Objects.requireNonNull(authSupport, "authSupport must not be null");
    }

    @Override
    public DashboardMetricsView execute(GetDashboardMetricsQueryModel query, AuthenticatedActor actor) {
        Objects.requireNonNull(query, "query must not be null");
        Objects.requireNonNull(actor, "actor must not be null");

        authSupport.ensureAdmin(actor);

        Optional<LocalDateTime> from = query.dateFrom()
            .map(date -> date.atStartOfDay());

        Optional<LocalDateTime> toExclusive = query.dateTo()
            .map(date -> date.plusDays(1).atStartOfDay());

        var criteria = new DashboardMetricsCriteria(from, toExclusive);

        return loadDashboardMetricsPort.load(criteria);
    }
}
