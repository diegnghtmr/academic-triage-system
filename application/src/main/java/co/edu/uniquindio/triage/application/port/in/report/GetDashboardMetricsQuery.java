package co.edu.uniquindio.triage.application.port.in.report;

import co.edu.uniquindio.triage.application.port.in.command.report.GetDashboardMetricsQueryModel;
import co.edu.uniquindio.triage.application.port.in.request.AuthenticatedRequestQuery;

public interface GetDashboardMetricsQuery extends AuthenticatedRequestQuery<GetDashboardMetricsQueryModel, DashboardMetricsView> {
}
