package co.edu.uniquindio.triage.infrastructure.adapter.out.ai;

import co.edu.uniquindio.triage.application.port.in.ai.AiClassificationSuggestion;
import co.edu.uniquindio.triage.application.port.in.ai.AiGeneratedSummary;
import co.edu.uniquindio.triage.application.port.in.request.RequestDetail;
import co.edu.uniquindio.triage.application.port.out.ai.AiAssistantPort;
import co.edu.uniquindio.triage.domain.enums.Priority;
import co.edu.uniquindio.triage.domain.model.RequestType;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class SpringAiAssistantAdapter implements AiAssistantPort {

    private final ChatClient chatClient;

    @Value("classpath:/prompts/ai-suggest-classification.st")
    private Resource suggestClassificationPrompt;

    @Value("classpath:/prompts/ai-generate-summary.st")
    private Resource generateSummaryPrompt;

    public SpringAiAssistantAdapter(ChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel)
            .defaultSystem("Eres un asistente inteligente para el sistema de triaje académico de la Universidad del Quindío.")
            .build();
    }

    @Override
    public AiClassificationSuggestion suggestClassification(String description, List<RequestType> activeRequestTypes) {
        String requestTypesList = activeRequestTypes.stream()
            .map(rt -> "- " + rt.getName() + ": " + rt.getDescription())
            .collect(Collectors.joining("\n"));

        AiClassificationResult result = chatClient.prompt()
            .user(u -> u.text(suggestClassificationPrompt)
                .param("requestTypes", requestTypesList)
                .param("description", description))
            .call()
            .entity(AiClassificationResult.class);

        return new AiClassificationSuggestion(
            result.requestTypeName(),
            Optional.empty(), // Resolved in the service
            result.priority(), // this maps to suggestedPriority in the record
            result.confidence(),
            result.reasoning()
        );
    }

    @Override
    public AiGeneratedSummary generateSummary(RequestDetail requestDetail) {
        String historyLog = requestDetail.history().stream()
            .map(h -> String.format("- [%s] %s: %s (por %s %s)",
                h.historyEntry().getTimestamp(),
                h.historyEntry().getAction(),
                h.historyEntry().getObservations(),
                h.performedBy().getFirstName(),
                h.performedBy().getLastName()))
            .collect(Collectors.joining("\n"));

        String summaryText = chatClient.prompt()
            .user(u -> u.text(generateSummaryPrompt)
                .param("description", requestDetail.request().getDescription())
                .param("type", requestDetail.requestType().getName())
                .param("status", requestDetail.request().getStatus())
                .param("priority", requestDetail.request().getPriority() != null ? requestDetail.request().getPriority() : "Sin asignar")
                .param("requester", requestDetail.requester().getFirstName() + " " + requestDetail.requester().getLastName())
                .param("assignedTo", requestDetail.assignedTo()
                    .map(u2 -> u2.getFirstName() + " " + u2.getLastName())
                    .orElse("Sin asignar"))
                .param("history", historyLog))
            .call()
            .content();

        return new AiGeneratedSummary(requestDetail.request().getId(), summaryText, LocalDateTime.now());
    }

    // Internal record for structured output
    private record AiClassificationResult(
        String requestTypeName,
        Priority priority,
        double confidence,
        String reasoning
    ) {}
}
