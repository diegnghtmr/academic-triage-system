package co.edu.uniquindio.triage.infrastructure.config;

import co.edu.uniquindio.triage.application.port.out.ai.AiAssistantPort;
import co.edu.uniquindio.triage.infrastructure.adapter.out.ai.NoOpAiAssistantAdapter;
import co.edu.uniquindio.triage.infrastructure.adapter.out.ai.SpringAiAssistantAdapter;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "app.ai", name = "provider", havingValue = "openai")
    @ConditionalOnBean(ChatClient.Builder.class)
    AiAssistantPort springAiAssistantPort(ChatClient.Builder chatClientBuilder) {
        return new SpringAiAssistantAdapter(chatClientBuilder);
    }

    @Bean
    @ConditionalOnMissingBean(AiAssistantPort.class)
    AiAssistantPort noOpAiAssistantPortFallback() {
        return new NoOpAiAssistantAdapter("AI is explicitly disabled or no provider was wired successfully");
    }
}
