package co.edu.uniquindio.triage.infrastructure.config;

import co.edu.uniquindio.triage.application.port.in.auth.LoginUseCase;
import co.edu.uniquindio.triage.application.port.in.auth.RegisterUseCase;
import co.edu.uniquindio.triage.application.port.in.request.AssignRequestUseCase;
import co.edu.uniquindio.triage.application.port.in.request.ClassifyRequestUseCase;
import co.edu.uniquindio.triage.application.port.in.request.CreateRequestUseCase;
import co.edu.uniquindio.triage.application.port.in.request.GetRequestDetailQuery;
import co.edu.uniquindio.triage.application.port.in.request.ListRequestsQuery;
import co.edu.uniquindio.triage.application.port.in.request.PrioritizeRequestUseCase;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadOriginChannelPort;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadRequestPort;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadRequestTypePort;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadUserAuthPort;
import co.edu.uniquindio.triage.application.port.out.persistence.NextRequestIdPort;
import co.edu.uniquindio.triage.application.port.out.persistence.SaveRequestPort;
import co.edu.uniquindio.triage.application.port.out.persistence.SaveUserPort;
import co.edu.uniquindio.triage.application.port.out.persistence.SearchRequestPort;
import co.edu.uniquindio.triage.application.port.out.security.PasswordEncoderPort;
import co.edu.uniquindio.triage.application.port.out.security.TokenProviderPort;
import co.edu.uniquindio.triage.application.service.auth.LoginService;
import co.edu.uniquindio.triage.application.service.auth.RegisterService;
import co.edu.uniquindio.triage.application.service.request.AssignRequestService;
import co.edu.uniquindio.triage.application.service.request.ClassifyRequestService;
import co.edu.uniquindio.triage.application.service.request.CreateRequestService;
import co.edu.uniquindio.triage.application.service.request.GetRequestDetailService;
import co.edu.uniquindio.triage.application.service.request.ListRequestsService;
import co.edu.uniquindio.triage.application.service.request.PrioritizeRequestService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class BeanConfiguration {

    @Bean
    RegisterUseCase registerUseCase(LoadUserAuthPort loadUserAuthPort,
                                    SaveUserPort saveUserPort,
                                    PasswordEncoderPort passwordEncoderPort) {
        return new RegisterService(loadUserAuthPort, saveUserPort, passwordEncoderPort);
    }

    @Bean
    LoginUseCase loginUseCase(LoadUserAuthPort loadUserAuthPort,
                              PasswordEncoderPort passwordEncoderPort,
                              TokenProviderPort tokenProviderPort) {
        return new LoginService(loadUserAuthPort, passwordEncoderPort, tokenProviderPort);
    }

    @Bean
    CreateRequestUseCase createRequestUseCase(NextRequestIdPort nextRequestIdPort,
                                              LoadRequestTypePort loadRequestTypePort,
                                              LoadOriginChannelPort loadOriginChannelPort,
                                              LoadUserAuthPort loadUserAuthPort,
                                              SaveRequestPort saveRequestPort) {
        return new CreateRequestService(
                nextRequestIdPort,
                loadRequestTypePort,
                loadOriginChannelPort,
                loadUserAuthPort,
                saveRequestPort
        );
    }

    @Bean
    ListRequestsQuery listRequestsQuery(SearchRequestPort searchRequestPort) {
        return new ListRequestsService(searchRequestPort);
    }

    @Bean
    GetRequestDetailQuery getRequestDetailQuery(LoadRequestPort loadRequestPort) {
        return new GetRequestDetailService(loadRequestPort);
    }

    @Bean
    ClassifyRequestUseCase classifyRequestUseCase(LoadRequestPort loadRequestPort,
                                                  LoadRequestTypePort loadRequestTypePort,
                                                  LoadOriginChannelPort loadOriginChannelPort,
                                                  LoadUserAuthPort loadUserAuthPort,
                                                  SaveRequestPort saveRequestPort) {
        return new ClassifyRequestService(
                loadRequestPort,
                loadRequestTypePort,
                loadOriginChannelPort,
                loadUserAuthPort,
                saveRequestPort
        );
    }

    @Bean
    PrioritizeRequestUseCase prioritizeRequestUseCase(LoadRequestPort loadRequestPort,
                                                      LoadRequestTypePort loadRequestTypePort,
                                                      LoadOriginChannelPort loadOriginChannelPort,
                                                      LoadUserAuthPort loadUserAuthPort,
                                                      SaveRequestPort saveRequestPort) {
        return new PrioritizeRequestService(
                loadRequestPort,
                loadRequestTypePort,
                loadOriginChannelPort,
                loadUserAuthPort,
                saveRequestPort
        );
    }

    @Bean
    AssignRequestUseCase assignRequestUseCase(LoadRequestPort loadRequestPort,
                                              LoadRequestTypePort loadRequestTypePort,
                                              LoadOriginChannelPort loadOriginChannelPort,
                                              LoadUserAuthPort loadUserAuthPort,
                                              SaveRequestPort saveRequestPort) {
        return new AssignRequestService(
                loadRequestPort,
                loadRequestTypePort,
                loadOriginChannelPort,
                loadUserAuthPort,
                saveRequestPort
        );
    }
}
