package co.edu.uniquindio.triage.application.port.in.report;

import co.edu.uniquindio.triage.domain.enums.Priority;
import co.edu.uniquindio.triage.domain.enums.RequestStatus;

import java.util.List;
import java.util.Map;

public record DashboardMetricsView(
    long totalRequests,
    Map<RequestStatus, Long> requestsByStatus,
    Map<String, Long> requestsByType,
    Map<Priority, Long> requestsByPriority,
    double averageResolutionTimeHours,
    List<TopResponsibleMetric> topResponsibles
) {
}
