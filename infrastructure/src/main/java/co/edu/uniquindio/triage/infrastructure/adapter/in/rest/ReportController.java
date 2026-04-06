package co.edu.uniquindio.triage.infrastructure.adapter.in.rest;

import co.edu.uniquindio.triage.application.port.in.command.report.GetDashboardMetricsQueryModel;
import co.edu.uniquindio.triage.application.port.in.report.GetDashboardMetricsQuery;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.report.DashboardMetricsResponse;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.mapper.ReportRestMapper;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.support.AuthenticatedActorMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final GetDashboardMetricsQuery getDashboardMetricsQuery;
    private final ReportRestMapper reportRestMapper;
    private final AuthenticatedActorMapper authenticatedActorMapper;

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DashboardMetricsResponse> getDashboardMetrics(
        @RequestParam(name = "dateFrom", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
        @RequestParam(name = "dateTo", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
        Authentication authentication
    ) {
        var actor = authenticatedActorMapper.toRequiredActor(authentication);
        var queryModel = new GetDashboardMetricsQueryModel(
            Optional.ofNullable(dateFrom),
            Optional.ofNullable(dateTo)
        );

        var metrics = getDashboardMetricsQuery.execute(queryModel, actor);
        return ResponseEntity.ok(reportRestMapper.toResponse(metrics));
    }
}
