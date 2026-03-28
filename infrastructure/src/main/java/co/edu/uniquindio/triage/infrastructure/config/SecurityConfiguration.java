package co.edu.uniquindio.triage.infrastructure.config;

import co.edu.uniquindio.triage.application.port.out.security.PasswordEncoderPort;
import co.edu.uniquindio.triage.application.port.out.security.TokenProviderPort;
import co.edu.uniquindio.triage.infrastructure.adapter.out.security.BcryptPasswordAdapter;
import co.edu.uniquindio.triage.infrastructure.adapter.out.security.JwtAuthenticationFilter;
import co.edu.uniquindio.triage.infrastructure.adapter.out.security.JwtTokenAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    private static final String[] DOCS_PATHS = {"/swagger-ui.html", "/swagger-ui/**", "/api-docs", "/api-docs/**"};

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    PasswordEncoderPort passwordEncoderPort(PasswordEncoder passwordEncoder) {
        return new BcryptPasswordAdapter(passwordEncoder);
    }

    @Bean
    TokenProviderPort tokenProviderPort(@Value("${app.jwt.secret}") String secret,
                                        @Value("${app.jwt.expiration-ms}") long expirationMs) {
        return new JwtTokenAdapter(secret, expirationMs);
    }

    @Bean
    JwtAuthenticationFilter jwtAuthenticationFilter(TokenProviderPort tokenProviderPort) {
        return new JwtAuthenticationFilter(tokenProviderPort);
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity,
                                            JwtAuthenticationFilter jwtAuthenticationFilter,
                                            ObjectMapper objectMapper,
                                            @Value("${app.docs.enabled:false}") boolean docsEnabled,
                                            @Value("${app.docs.public-enabled:false}") boolean publicDocsEnabled) throws Exception {
        var http = httpSecurity
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> {
                    authorize.requestMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/login").permitAll();
                    authorize.requestMatchers("/actuator/health").permitAll();
                    authorize.requestMatchers(HttpMethod.GET,
                            "/api/v1/catalogs/request-types",
                            "/api/v1/catalogs/request-types/*",
                            "/api/v1/catalogs/origin-channels",
                            "/api/v1/catalogs/origin-channels/*").authenticated();
                    authorize.requestMatchers(HttpMethod.POST,
                            "/api/v1/catalogs/request-types",
                            "/api/v1/catalogs/origin-channels").hasRole("ADMIN");
                    authorize.requestMatchers(HttpMethod.PUT,
                            "/api/v1/catalogs/request-types/*",
                            "/api/v1/catalogs/origin-channels/*").hasRole("ADMIN");

                    if (docsEnabled && publicDocsEnabled) {
                        authorize.requestMatchers(DOCS_PATHS).permitAll();
                    } else if (docsEnabled) {
                        authorize.requestMatchers(DOCS_PATHS).hasRole("ADMIN");
                    } else {
                        authorize.requestMatchers(DOCS_PATHS).denyAll();
                    }

                    authorize.anyRequest().authenticated();
                })
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) ->
                                writeError(response, objectMapper, HttpStatus.UNAUTHORIZED, "Invalid or expired JWT token."))
                        .accessDeniedHandler((request, response, exception) ->
                                writeError(response, objectMapper, HttpStatus.FORBIDDEN, "Insufficient permissions for this operation.")))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                ;

        return http.build();
    }

    private void writeError(jakarta.servlet.http.HttpServletResponse response,
                            ObjectMapper objectMapper,
                            HttpStatus status,
                            String message) throws java.io.IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        var problemDetail = ProblemDetail.forStatusAndDetail(status, message);
        problemDetail.setTitle(status.getReasonPhrase());
        objectMapper.writeValue(response.getOutputStream(), problemDetail);
    }
}
