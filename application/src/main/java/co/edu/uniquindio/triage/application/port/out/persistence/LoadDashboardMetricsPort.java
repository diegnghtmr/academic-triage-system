package co.edu.uniquindio.triage.application.port.out.persistence;

import co.edu.uniquindio.triage.application.port.in.report.DashboardMetricsView;

public interface LoadDashboardMetricsPort {
    DashboardMetricsView load(DashboardMetricsCriteria criteria);
}
