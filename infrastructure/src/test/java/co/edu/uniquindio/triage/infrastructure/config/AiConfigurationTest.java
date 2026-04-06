package co.edu.uniquindio.triage.infrastructure.config;

import co.edu.uniquindio.triage.application.port.out.ai.AiAssistantPort;
import co.edu.uniquindio.triage.infrastructure.adapter.out.ai.NoOpAiAssistantAdapter;
import co.edu.uniquindio.triage.infrastructure.adapter.out.ai.SpringAiAssistantAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(AiConfiguration.class));

    @Test
    void whenApiKeyIsMissing_thenNoOpAdapterIsWired() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(AiAssistantPort.class);
            assertThat(context.getBean(AiAssistantPort.class)).isInstanceOf(NoOpAiAssistantAdapter.class);
        });
    }

    @Test
    void whenApiKeyIsNone_thenNoOpAdapterIsWired() {
        contextRunner.withPropertyValues("spring.ai.openai.api-key=none")
            .run(context -> {
                assertThat(context).hasSingleBean(AiAssistantPort.class);
                assertThat(context.getBean(AiAssistantPort.class)).isInstanceOf(NoOpAiAssistantAdapter.class);
            });
    }

    @Test
    void whenApiKeyIsProvided_thenSpringAiAdapterIsWired() {
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        ChatClient chatClient = mock(ChatClient.class);
        when(builder.defaultSystem(anyString())).thenReturn(builder);
        when(builder.build()).thenReturn(chatClient);

        contextRunner.withPropertyValues("spring.ai.openai.api-key=test-key")
            .withBean(ChatClient.Builder.class, () -> builder)
            .run(context -> {
                assertThat(context).hasSingleBean(AiAssistantPort.class);
                assertThat(context.getBean(AiAssistantPort.class)).isInstanceOf(SpringAiAssistantAdapter.class);
            });
    }
}
