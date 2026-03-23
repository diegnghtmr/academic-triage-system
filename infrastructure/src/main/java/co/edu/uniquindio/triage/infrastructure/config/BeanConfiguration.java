package co.edu.uniquindio.triage.infrastructure.config;

import co.edu.uniquindio.triage.application.port.in.auth.LoginUseCase;
import co.edu.uniquindio.triage.application.port.in.auth.RegisterUseCase;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadUserAuthPort;
import co.edu.uniquindio.triage.application.port.out.persistence.SaveUserPort;
import co.edu.uniquindio.triage.application.port.out.security.PasswordEncoderPort;
import co.edu.uniquindio.triage.application.port.out.security.TokenProviderPort;
import co.edu.uniquindio.triage.application.service.auth.LoginService;
import co.edu.uniquindio.triage.application.service.auth.RegisterService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfiguration {

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
}
