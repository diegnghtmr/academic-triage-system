package co.edu.uniquindio.triage.infrastructure.config;

import co.edu.uniquindio.triage.application.port.out.ai.AiAssistantPort;
import co.edu.uniquindio.triage.infrastructure.adapter.out.ai.NoOpAiAssistantAdapter;
import co.edu.uniquindio.triage.infrastructure.adapter.out.ai.SpringAiAssistantAdapter;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfiguration {

    @Bean
    @ConditionalOnProperty(name = "spring.ai.openai.api-key")
    @ConditionalOnExpression("!'${spring.ai.openai.api-key:}'.equals('none') && !'${spring.ai.openai.api-key:}'.isEmpty()")
    AiAssistantPort springAiAssistantPort(ChatClient.Builder chatClientBuilder) {
        return new SpringAiAssistantAdapter(chatClientBuilder);
    }

    @Bean
    @ConditionalOnExpression("'${spring.ai.openai.api-key:}'.isEmpty() || '${spring.ai.openai.api-key:}'.equals('none')")
    AiAssistantPort noOpAiAssistantPortFallback() {
        return new NoOpAiAssistantAdapter("API key de OpenAI no configurada o explícitamente deshabilitada");
    }
}
