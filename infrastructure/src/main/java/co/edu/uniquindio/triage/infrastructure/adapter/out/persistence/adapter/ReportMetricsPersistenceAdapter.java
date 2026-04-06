package co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.adapter;

import co.edu.uniquindio.triage.application.port.in.report.DashboardMetricsView;
import co.edu.uniquindio.triage.application.port.in.report.TopResponsibleMetric;
import co.edu.uniquindio.triage.application.port.out.persistence.DashboardMetricsCriteria;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadDashboardMetricsPort;
import co.edu.uniquindio.triage.domain.enums.Priority;
import co.edu.uniquindio.triage.domain.enums.RequestStatus;
import co.edu.uniquindio.triage.domain.model.id.UserId;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.repository.RequestHistoryJpaRepository;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.repository.RequestJpaRepository;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ReportMetricsPersistenceAdapter implements LoadDashboardMetricsPort {

    private final RequestJpaRepository requestJpaRepository;
    private final RequestHistoryJpaRepository requestHistoryJpaRepository;
    private final UserJpaRepository userJpaRepository;

    @Override
    public DashboardMetricsView load(DashboardMetricsCriteria criteria) {
        LocalDateTime from = criteria.from().orElse(null);
        LocalDateTime to = criteria.toExclusive().orElse(null);

        long totalRequests = requestJpaRepository.countByRegistrationDateTime(from, to);

        Map<RequestStatus, Long> requestsByStatus = requestJpaRepository.countByStatusAndRegistrationDateTime(from, to)
            .stream()
            .collect(Collectors.toMap(
                row -> RequestStatus.valueOf((String) row[0]),
                row -> (Long) row[1]
            ));

        Map<String, Long> requestsByType = requestJpaRepository.countByTypeNameAndRegistrationDateTime(from, to)
            .stream()
            .collect(Collectors.toMap(
                row -> (String) row[0],
                row -> (Long) row[1]
            ));

        Map<Priority, Long> requestsByPriority = requestJpaRepository.countByPriorityAndRegistrationDateTime(from, to)
            .stream()
            .filter(row -> row[0] != null)
            .collect(Collectors.toMap(
                row -> Priority.valueOf((String) row[0]),
                row -> (Long) row[1]
            ));

        Double avgResolutionTime = requestHistoryJpaRepository.calculateAverageResolutionTimeHours(from, to);
        double averageResolutionTimeHours = avgResolutionTime != null ? avgResolutionTime : 0.0;

        var topResponsibleRows = requestHistoryJpaRepository.findTopResponsiblesByClosedRequests(from, to);
        var userIds = topResponsibleRows.stream()
            .map(row -> (Long) row[0])
            .filter(Objects::nonNull)
            .toList();

        var usersById = userJpaRepository.findAllById(userIds).stream()
            .collect(Collectors.toMap(user -> user.getId(), Function.identity()));

        List<TopResponsibleMetric> topResponsibles = topResponsibleRows
            .stream()
            .map(row -> {
                Long userId = (Long) row[0];
                long closedCount = (Long) row[1];
                var user = usersById.get(userId);
                if (user == null) {
                    return null;
                }

                return new TopResponsibleMetric(
                        new UserId(user.getId()),
                        user.getUsername(),
                        user.getFirstName(),
                        user.getLastName(),
                        user.getIdentification(),
                        user.getEmail(),
                        co.edu.uniquindio.triage.domain.enums.Role.valueOf(user.getRole()),
                        user.isActive(),
                        closedCount
                    );
            })
            .filter(Objects::nonNull)
            .toList();

        return new DashboardMetricsView(
            totalRequests,
            requestsByStatus,
            requestsByType,
            requestsByPriority,
            averageResolutionTimeHours,
            topResponsibles
        );
    }
}
