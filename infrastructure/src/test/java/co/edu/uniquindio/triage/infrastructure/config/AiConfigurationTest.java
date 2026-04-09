package co.edu.uniquindio.triage.infrastructure.config;

import co.edu.uniquindio.triage.application.port.out.ai.AiAssistantPort;
import co.edu.uniquindio.triage.infrastructure.adapter.out.ai.NoOpAiAssistantAdapter;
import co.edu.uniquindio.triage.infrastructure.adapter.out.ai.SpringAiAssistantAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AiConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(AiConfiguration.class));

    @Test
    void whenProviderIsMissing_thenNoOpAdapterIsWired() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(AiAssistantPort.class);
            assertThat(context.getBean(AiAssistantPort.class)).isInstanceOf(NoOpAiAssistantAdapter.class);
        });
    }

    @Test
    void whenProviderIsNone_thenNoOpAdapterIsWired() {
        contextRunner.withPropertyValues("app.ai.provider=none")
            .run(context -> {
                assertThat(context).hasSingleBean(AiAssistantPort.class);
                assertThat(context.getBean(AiAssistantPort.class)).isInstanceOf(NoOpAiAssistantAdapter.class);
            });
    }

    @Test
    void whenProviderIsOpenAiAndChatModelExists_thenSpringAiAdapterIsWired() {
        ChatModel chatModel = mock(ChatModel.class);

        contextRunner.withPropertyValues("app.ai.provider=openai")
            .withBean(ChatModel.class, () -> chatModel)
            .run(context -> {
                assertThat(context).hasSingleBean(AiAssistantPort.class);
                assertThat(context.getBean(AiAssistantPort.class)).isInstanceOf(SpringAiAssistantAdapter.class);
            });
    }

    @Test
    void whenProviderIsOpenAiButChatModelIsMissing_thenNoOpAdapterIsWiredAsFallback() {
        contextRunner.withPropertyValues("app.ai.provider=openai")
            .run(context -> {
                assertThat(context).hasSingleBean(AiAssistantPort.class);
                assertThat(context.getBean(AiAssistantPort.class)).isInstanceOf(NoOpAiAssistantAdapter.class);
            });
    }
}
