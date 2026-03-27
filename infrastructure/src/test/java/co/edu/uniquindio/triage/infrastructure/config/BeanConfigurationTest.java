package co.edu.uniquindio.triage.infrastructure.config;

import co.edu.uniquindio.triage.application.service.request.AttendRequestService;
import co.edu.uniquindio.triage.application.service.request.CloseRequestService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class BeanConfigurationTest {

    private final BeanConfiguration beanConfiguration = new BeanConfiguration();

    @Test
    void attendRequestUseCaseMustWireAttendRequestService() {
        var useCase = beanConfiguration.attendRequestUseCase(
                mock(co.edu.uniquindio.triage.application.port.out.persistence.LoadRequestPort.class),
                mock(co.edu.uniquindio.triage.application.port.out.persistence.LoadRequestTypePort.class),
                mock(co.edu.uniquindio.triage.application.port.out.persistence.LoadOriginChannelPort.class),
                mock(co.edu.uniquindio.triage.application.port.out.persistence.LoadUserAuthPort.class),
                mock(co.edu.uniquindio.triage.application.port.out.persistence.SaveRequestPort.class)
        );

        assertThat(useCase).isInstanceOf(AttendRequestService.class);
    }

    @Test
    void closeRequestUseCaseMustWireCloseRequestService() {
        var useCase = beanConfiguration.closeRequestUseCase(
                mock(co.edu.uniquindio.triage.application.port.out.persistence.LoadRequestPort.class),
                mock(co.edu.uniquindio.triage.application.port.out.persistence.LoadRequestTypePort.class),
                mock(co.edu.uniquindio.triage.application.port.out.persistence.LoadOriginChannelPort.class),
                mock(co.edu.uniquindio.triage.application.port.out.persistence.LoadUserAuthPort.class),
                mock(co.edu.uniquindio.triage.application.port.out.persistence.SaveRequestPort.class)
        );

        assertThat(useCase).isInstanceOf(CloseRequestService.class);
    }
}
