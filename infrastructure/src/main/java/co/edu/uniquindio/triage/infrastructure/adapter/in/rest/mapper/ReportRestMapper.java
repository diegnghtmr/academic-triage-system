package co.edu.uniquindio.triage.infrastructure.adapter.in.rest.mapper;

import co.edu.uniquindio.triage.application.port.in.report.DashboardMetricsView;
import co.edu.uniquindio.triage.application.port.in.report.TopResponsibleMetric;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.report.DashboardMetricsResponse;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.report.TopResponsibleResponse;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.user.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ReportRestMapper {

    private final UserRestMapper userRestMapper;

    public DashboardMetricsResponse toResponse(DashboardMetricsView view) {
        return new DashboardMetricsResponse(
            view.totalRequests(),
            view.requestsByStatus().entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().name(), Map.Entry::getValue)),
            view.requestsByType(),
            view.requestsByPriority().entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().name(), Map.Entry::getValue)),
            view.averageResolutionTimeHours(),
            view.topResponsibles().stream()
                .map(this::toTopResponsibleResponse)
                .toList()
        );
    }

    private TopResponsibleResponse toTopResponsibleResponse(TopResponsibleMetric metric) {
        UserResponse user = new UserResponse(
            metric.userId().value(),
            metric.username(),
            metric.firstName(),
            metric.lastName(),
            metric.identification(),
            metric.email(),
            metric.role().name(),
            metric.active()
        );
        return new TopResponsibleResponse(user, metric.closedRequestsCount());
    }
}
