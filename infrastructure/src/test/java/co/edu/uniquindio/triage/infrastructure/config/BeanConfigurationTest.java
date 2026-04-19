package co.edu.uniquindio.triage.infrastructure.config;

import co.edu.uniquindio.triage.application.service.catalog.CreateOriginChannelService;
import co.edu.uniquindio.triage.application.service.catalog.CreateRequestTypeService;
import co.edu.uniquindio.triage.application.service.catalog.ListOriginChannelsService;
import co.edu.uniquindio.triage.application.service.catalog.ListRequestTypesService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class BeanConfigurationTest {

    private final BeanConfiguration beanConfiguration = new BeanConfiguration();

    @Test
    void listRequestTypesQueryMustWireListRequestTypesService() {
        var query = beanConfiguration.listRequestTypesQuery(
                mock(co.edu.uniquindio.triage.application.port.out.persistence.LoadRequestTypePort.class)
        );

        assertThat(query).isInstanceOf(ListRequestTypesService.class);
    }

    @Test
    void createRequestTypeUseCaseMustWireCreateRequestTypeService() {
        var useCase = beanConfiguration.createRequestTypeUseCase(
                mock(co.edu.uniquindio.triage.application.port.out.persistence.LoadRequestTypePort.class),
                mock(co.edu.uniquindio.triage.application.port.out.persistence.SaveRequestTypePort.class)
        );

        assertThat(useCase).isInstanceOf(CreateRequestTypeService.class);
    }

    @Test
    void listOriginChannelsQueryMustWireListOriginChannelsService() {
        var query = beanConfiguration.listOriginChannelsQuery(
                mock(co.edu.uniquindio.triage.application.port.out.persistence.LoadOriginChannelPort.class)
        );

        assertThat(query).isInstanceOf(ListOriginChannelsService.class);
    }

    @Test
    void createOriginChannelUseCaseMustWireCreateOriginChannelService() {
        var useCase = beanConfiguration.createOriginChannelUseCase(
                mock(co.edu.uniquindio.triage.application.port.out.persistence.LoadOriginChannelPort.class),
                mock(co.edu.uniquindio.triage.application.port.out.persistence.SaveOriginChannelPort.class)
        );

        assertThat(useCase).isInstanceOf(CreateOriginChannelService.class);
    }
}
