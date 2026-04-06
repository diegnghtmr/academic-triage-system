package co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.report;

import java.util.List;
import java.util.Map;

public record DashboardMetricsResponse(
    long totalRequests,
    Map<String, Long> requestsByStatus,
    Map<String, Long> requestsByType,
    Map<String, Long> requestsByPriority,
    double averageResolutionTimeHours,
    List<TopResponsibleResponse> topResponsibles
) {
}
