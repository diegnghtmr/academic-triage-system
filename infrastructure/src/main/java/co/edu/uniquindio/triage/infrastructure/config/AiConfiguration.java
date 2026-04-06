package co.edu.uniquindio.triage.infrastructure.config;

import co.edu.uniquindio.triage.application.port.out.ai.AiAssistantPort;
import co.edu.uniquindio.triage.infrastructure.adapter.out.ai.NoOpAiAssistantAdapter;
import co.edu.uniquindio.triage.infrastructure.adapter.out.ai.SpringAiAssistantAdapter;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.ObjectProvider;

@Configuration
public class AiConfiguration {

    @Bean
    AiAssistantPort aiAssistantPort(@Value("${app.ai.provider:none}") String provider,
                                    ObjectProvider<ChatModel> chatModelProvider) {
        if ("openai".equalsIgnoreCase(provider)) {
            var chatModel = chatModelProvider.getIfAvailable();
            if (chatModel != null) {
                return new SpringAiAssistantAdapter(chatModel);
            }
        }

        return new NoOpAiAssistantAdapter("AI is explicitly disabled or no provider was wired successfully");
    }
}
