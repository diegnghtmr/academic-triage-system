package co.edu.uniquindio.triage.infrastructure.config;

import co.edu.uniquindio.triage.application.port.in.auth.LoginUseCase;
import co.edu.uniquindio.triage.application.port.in.auth.RegisterUseCase;
import co.edu.uniquindio.triage.application.port.in.catalog.CreateOriginChannelUseCase;
import co.edu.uniquindio.triage.application.port.in.catalog.CreateRequestTypeUseCase;
import co.edu.uniquindio.triage.application.port.in.catalog.GetOriginChannelQuery;
import co.edu.uniquindio.triage.application.port.in.catalog.GetRequestTypeQuery;
import co.edu.uniquindio.triage.application.port.in.catalog.ListOriginChannelsQuery;
import co.edu.uniquindio.triage.application.port.in.catalog.ListRequestTypesQuery;
import co.edu.uniquindio.triage.application.port.in.catalog.UpdateOriginChannelUseCase;
import co.edu.uniquindio.triage.application.port.in.catalog.UpdateRequestTypeUseCase;
import co.edu.uniquindio.triage.application.port.in.request.AssignRequestUseCase;
import co.edu.uniquindio.triage.application.port.in.request.AttendRequestUseCase;
import co.edu.uniquindio.triage.application.port.in.request.CancelRequestUseCase;
import co.edu.uniquindio.triage.application.port.in.request.ClassifyRequestUseCase;
import co.edu.uniquindio.triage.application.port.in.request.CloseRequestUseCase;
import co.edu.uniquindio.triage.application.port.in.request.CreateRequestUseCase;
import co.edu.uniquindio.triage.application.port.in.request.GetRequestDetailQuery;
import co.edu.uniquindio.triage.application.port.in.request.ListRequestsQuery;
import co.edu.uniquindio.triage.application.port.in.request.PrioritizeRequestUseCase;
import co.edu.uniquindio.triage.application.port.in.request.RejectRequestUseCase;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadOriginChannelPort;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadRequestPort;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadRequestTypePort;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadUserAuthPort;
import co.edu.uniquindio.triage.application.port.out.persistence.NextRequestIdPort;
import co.edu.uniquindio.triage.application.port.out.persistence.SaveOriginChannelPort;
import co.edu.uniquindio.triage.application.port.out.persistence.SaveRequestPort;
import co.edu.uniquindio.triage.application.port.out.persistence.SaveRequestTypePort;
import co.edu.uniquindio.triage.application.port.out.persistence.SaveUserPort;
import co.edu.uniquindio.triage.application.port.out.persistence.SearchRequestPort;
import co.edu.uniquindio.triage.application.port.out.security.PasswordEncoderPort;
import co.edu.uniquindio.triage.application.port.out.security.TokenProviderPort;
import co.edu.uniquindio.triage.application.service.auth.LoginService;
import co.edu.uniquindio.triage.application.service.auth.RegisterService;
import co.edu.uniquindio.triage.application.service.catalog.CreateOriginChannelService;
import co.edu.uniquindio.triage.application.service.catalog.CreateRequestTypeService;
import co.edu.uniquindio.triage.application.service.catalog.GetOriginChannelService;
import co.edu.uniquindio.triage.application.service.catalog.GetRequestTypeService;
import co.edu.uniquindio.triage.application.service.catalog.ListOriginChannelsService;
import co.edu.uniquindio.triage.application.service.catalog.ListRequestTypesService;
import co.edu.uniquindio.triage.application.service.catalog.UpdateOriginChannelService;
import co.edu.uniquindio.triage.application.service.catalog.UpdateRequestTypeService;
import co.edu.uniquindio.triage.application.service.request.AssignRequestService;
import co.edu.uniquindio.triage.application.service.request.AttendRequestService;
import co.edu.uniquindio.triage.application.service.request.CancelRequestService;
import co.edu.uniquindio.triage.application.service.request.ClassifyRequestService;
import co.edu.uniquindio.triage.application.service.request.CloseRequestService;
import co.edu.uniquindio.triage.application.service.request.CreateRequestService;
import co.edu.uniquindio.triage.application.service.request.GetRequestDetailService;
import co.edu.uniquindio.triage.application.service.request.ListRequestsService;
import co.edu.uniquindio.triage.application.service.request.PrioritizeRequestService;
import co.edu.uniquindio.triage.application.service.request.RejectRequestService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class BeanConfiguration {

    @Bean
    ListRequestTypesQuery listRequestTypesQuery(LoadRequestTypePort loadRequestTypePort) {
        return new ListRequestTypesService(loadRequestTypePort);
    }

    @Bean
    GetRequestTypeQuery getRequestTypeQuery(LoadRequestTypePort loadRequestTypePort) {
        return new GetRequestTypeService(loadRequestTypePort);
    }

    @Bean
    CreateRequestTypeUseCase createRequestTypeUseCase(LoadRequestTypePort loadRequestTypePort,
                                                      SaveRequestTypePort saveRequestTypePort) {
        return new CreateRequestTypeService(loadRequestTypePort, saveRequestTypePort);
    }

    @Bean
    UpdateRequestTypeUseCase updateRequestTypeUseCase(LoadRequestTypePort loadRequestTypePort,
                                                      SaveRequestTypePort saveRequestTypePort) {
        return new UpdateRequestTypeService(loadRequestTypePort, saveRequestTypePort);
    }

    @Bean
    ListOriginChannelsQuery listOriginChannelsQuery(LoadOriginChannelPort loadOriginChannelPort) {
        return new ListOriginChannelsService(loadOriginChannelPort);
    }

    @Bean
    GetOriginChannelQuery getOriginChannelQuery(LoadOriginChannelPort loadOriginChannelPort) {
        return new GetOriginChannelService(loadOriginChannelPort);
    }

    @Bean
    CreateOriginChannelUseCase createOriginChannelUseCase(LoadOriginChannelPort loadOriginChannelPort,
                                                          SaveOriginChannelPort saveOriginChannelPort) {
        return new CreateOriginChannelService(loadOriginChannelPort, saveOriginChannelPort);
    }

    @Bean
    UpdateOriginChannelUseCase updateOriginChannelUseCase(LoadOriginChannelPort loadOriginChannelPort,
                                                          SaveOriginChannelPort saveOriginChannelPort) {
        return new UpdateOriginChannelService(loadOriginChannelPort, saveOriginChannelPort);
    }

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

    @Bean
    AttendRequestUseCase attendRequestUseCase(LoadRequestPort loadRequestPort,
                                              LoadRequestTypePort loadRequestTypePort,
                                              LoadOriginChannelPort loadOriginChannelPort,
                                              LoadUserAuthPort loadUserAuthPort,
                                              SaveRequestPort saveRequestPort) {
        return new AttendRequestService(
                loadRequestPort,
                loadRequestTypePort,
                loadOriginChannelPort,
                loadUserAuthPort,
                saveRequestPort
        );
    }

    @Bean
    CloseRequestUseCase closeRequestUseCase(LoadRequestPort loadRequestPort,
                                            LoadRequestTypePort loadRequestTypePort,
                                            LoadOriginChannelPort loadOriginChannelPort,
                                            LoadUserAuthPort loadUserAuthPort,
                                            SaveRequestPort saveRequestPort) {
        return new CloseRequestService(
                loadRequestPort,
                loadRequestTypePort,
                loadOriginChannelPort,
                loadUserAuthPort,
                saveRequestPort
        );
    }

    @Bean
    CancelRequestUseCase cancelRequestUseCase(LoadRequestPort loadRequestPort,
                                              LoadRequestTypePort loadRequestTypePort,
                                              LoadOriginChannelPort loadOriginChannelPort,
                                              LoadUserAuthPort loadUserAuthPort,
                                              SaveRequestPort saveRequestPort) {
        return new CancelRequestService(
                loadRequestPort,
                loadRequestTypePort,
                loadOriginChannelPort,
                loadUserAuthPort,
                saveRequestPort
        );
    }

    @Bean
    RejectRequestUseCase rejectRequestUseCase(LoadRequestPort loadRequestPort,
                                              LoadRequestTypePort loadRequestTypePort,
                                              LoadOriginChannelPort loadOriginChannelPort,
                                              LoadUserAuthPort loadUserAuthPort,
                                              SaveRequestPort saveRequestPort) {
        return new RejectRequestService(
                loadRequestPort,
                loadRequestTypePort,
                loadOriginChannelPort,
                loadUserAuthPort,
                saveRequestPort
        );
    }
}
