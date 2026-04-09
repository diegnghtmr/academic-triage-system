package co.edu.uniquindio.triage.infrastructure.adapter.out.security;

import co.edu.uniquindio.triage.application.port.out.persistence.LoadUserAuthPort;
import co.edu.uniquindio.triage.application.port.out.security.AuthenticatedUserPayload;
import co.edu.uniquindio.triage.application.port.out.security.TokenProviderPort;
import co.edu.uniquindio.triage.domain.enums.Role;
import co.edu.uniquindio.triage.domain.model.Email;
import co.edu.uniquindio.triage.domain.model.Identification;
import co.edu.uniquindio.triage.domain.model.PasswordHash;
import co.edu.uniquindio.triage.domain.model.User;
import co.edu.uniquindio.triage.domain.model.Username;
import co.edu.uniquindio.triage.domain.model.id.UserId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {

    private TokenProviderPort tokenProviderPort;
    private LoadUserAuthPort loadUserAuthPort;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        tokenProviderPort = mock(TokenProviderPort.class);
        loadUserAuthPort = mock(LoadUserAuthPort.class);
        filter = new JwtAuthenticationFilter(tokenProviderPort, loadUserAuthPort);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validBearerTokenMustPopulateSecurityContextWithCurrentPrincipalState() throws Exception {
        var userId = 7L;
        var token = "valid-token";
        var payload = new AuthenticatedUserPayload(userId, "jperez-old", Role.STUDENT);
        
        var currentUser = User.reconstitute(
                UserId.of(userId),
                Username.of("jperez-new"),
                "Juan",
                "Perez",
                PasswordHash.of("hash-valid-length-at-least-some-chars"),
                Identification.of("12345678"),
                Email.of("juan@example.com"),
                Role.STAFF,
                true
        );

        when(tokenProviderPort.parse(token)).thenReturn(payload);
        when(loadUserAuthPort.loadById(UserId.of(userId))).thenReturn(Optional.of(currentUser));

        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        var response = new MockHttpServletResponse();
        var filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isInstanceOf(AuthenticatedUser.class);
        var principal = (AuthenticatedUser) authentication.getPrincipal();
        
        // Debe usar el estado RECIÉN CARGADO, no el del payload del token
        assertThat(principal.username()).isEqualTo("jperez-new");
        assertThat(principal.role()).isEqualTo(Role.STAFF);
        assertThat(authentication.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_STAFF");
    }

    @Test
    void inactiveUserMustClearSecurityContext() throws Exception {
        var userId = 7L;
        var token = "valid-token";
        var payload = new AuthenticatedUserPayload(userId, "jperez", Role.STAFF);
        
        var inactiveUser = User.reconstitute(
                UserId.of(userId),
                Username.of("jperez"),
                "Juan",
                "Perez",
                PasswordHash.of("hash-valid-length-at-least-some-chars"),
                Identification.of("12345678"),
                Email.of("juan@example.com"),
                Role.STAFF,
                false // Inactivo
        );

        when(tokenProviderPort.parse(token)).thenReturn(payload);
        when(loadUserAuthPort.loadById(UserId.of(userId))).thenReturn(Optional.of(inactiveUser));

        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void nonExistentUserMustClearSecurityContext() throws Exception {
        var userId = 7L;
        var token = "valid-token";
        var payload = new AuthenticatedUserPayload(userId, "jperez", Role.STAFF);

        when(tokenProviderPort.parse(token)).thenReturn(payload);
        when(loadUserAuthPort.loadById(UserId.of(userId))).thenReturn(Optional.empty());

        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void invalidBearerTokenMustLeaveSecurityContextEmpty() throws Exception {
        var token = "invalid-token";
        when(tokenProviderPort.parse(token)).thenThrow(new RuntimeException("Invalid token"));

        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void missingAuthorizationHeaderMustNotInvokeTokenParsing() throws Exception {
        var request = new MockHttpServletRequest();
        var chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        verifyNoInteractions(tokenProviderPort, loadUserAuthPort);
        assertThat(chain.getRequest()).isSameAs(request);
    }

    @Test
    void nonBearerAuthorizationSchemeMustNotInvokeTokenParsing() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic abc123");
        var chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        verifyNoInteractions(tokenProviderPort, loadUserAuthPort);
        assertThat(chain.getRequest()).isSameAs(request);
    }

    @Test
    void mustSkipJwtProcessingWhenSecurityContextAlreadyAuthenticated() throws Exception {
        var preExisting = new UsernamePasswordAuthenticationToken(
                "legacy-principal",
                "credentials",
                List.of(new SimpleGrantedAuthority("ROLE_SERVICE"))
        );
        SecurityContextHolder.getContext().setAuthentication(preExisting);

        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer should-not-parse");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        verify(tokenProviderPort, never()).parse(anyString());
        verifyNoInteractions(loadUserAuthPort);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(preExisting);
    }
}
