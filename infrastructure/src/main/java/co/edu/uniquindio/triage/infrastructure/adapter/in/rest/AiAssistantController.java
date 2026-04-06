package co.edu.uniquindio.triage.infrastructure.adapter.in.rest;

import co.edu.uniquindio.triage.application.port.in.ai.GenerateSummaryUseCase;
import co.edu.uniquindio.triage.application.port.in.ai.SuggestClassificationUseCase;
import co.edu.uniquindio.triage.application.port.in.command.ai.GenerateSummaryQueryModel;
import co.edu.uniquindio.triage.application.port.in.command.ai.SuggestClassificationCommand;
import co.edu.uniquindio.triage.domain.model.id.RequestId;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.ai.AiClassificationRequest;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.ai.AiClassificationResponse;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.ai.AiSummaryResponse;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.mapper.AiRestMapper;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.support.AuthenticatedActorMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiAssistantController {

    private final SuggestClassificationUseCase suggestClassificationUseCase;
    private final GenerateSummaryUseCase generateSummaryUseCase;
    private final AiRestMapper aiRestMapper;
    private final AuthenticatedActorMapper authenticatedActorMapper;

    @PostMapping("/suggest-classification")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<AiClassificationResponse> suggestClassification(
        @Valid @RequestBody AiClassificationRequest request,
        Authentication authentication
    ) {
        var actor = authenticatedActorMapper.toRequiredActor(authentication);
        var command = new SuggestClassificationCommand(request.description());
        var suggestion = suggestClassificationUseCase.execute(command, actor);
        return ResponseEntity.ok(aiRestMapper.toResponse(suggestion));
    }

    @GetMapping("/summarize/{requestId}")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<AiSummaryResponse> summarize(
        @PathVariable(name = "requestId") Long requestId,
        Authentication authentication
    ) {
        var actor = authenticatedActorMapper.toRequiredActor(authentication);
        var query = new GenerateSummaryQueryModel(RequestId.of(requestId));
        var summary = generateSummaryUseCase.execute(query, actor);
        return ResponseEntity.ok(aiRestMapper.toResponse(summary));
    }
}
