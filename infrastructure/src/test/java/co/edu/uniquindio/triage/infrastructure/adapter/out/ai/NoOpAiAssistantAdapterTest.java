package co.edu.uniquindio.triage.infrastructure.adapter.out.ai;

import co.edu.uniquindio.triage.application.port.out.ai.AiAssistantPort;
import co.edu.uniquindio.triage.domain.exception.AiServiceUnavailableException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NoOpAiAssistantAdapterTest {

    @Test
    void noOpAdapter_ShouldThrowAiServiceUnavailableException() {
        AiAssistantPort adapter = new NoOpAiAssistantAdapter("Configuracion faltante");
        
        assertThatThrownBy(() -> adapter.suggestClassification("desc", List.of()))
            .isInstanceOf(AiServiceUnavailableException.class)
            .hasMessageContaining("Configuracion faltante");

        assertThatThrownBy(() -> adapter.generateSummary(null))
            .isInstanceOf(AiServiceUnavailableException.class)
            .hasMessageContaining("Configuracion faltante");
    }
}
