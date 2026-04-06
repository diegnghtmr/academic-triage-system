package co.edu.uniquindio.triage.infrastructure.adapter.out.security;

import co.edu.uniquindio.triage.application.port.out.security.AuthToken;
import co.edu.uniquindio.triage.application.port.out.security.AuthenticatedUserPayload;
import co.edu.uniquindio.triage.application.port.out.security.TokenProviderPort;
import co.edu.uniquindio.triage.domain.enums.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationFilterTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validBearerTokenMustPopulateSecurityContextWithPrincipalAndRole() throws Exception {
        var filter = new JwtAuthenticationFilter(new StubTokenProviderPort(new AuthenticatedUserPayload(7L, "jperez", Role.STAFF)));
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-token");
        var response = new MockHttpServletResponse();
        var filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isInstanceOf(AuthenticatedUser.class);
        var principal = (AuthenticatedUser) authentication.getPrincipal();
        assertThat(principal.username()).isEqualTo("jperez");
        assertThat(principal.role()).isEqualTo(Role.STAFF);
        assertThat(authentication.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_STAFF");
        assertThat(authentication.getDetails()).isNotNull();
    }

    @Test
    void invalidBearerTokenMustLeaveSecurityContextEmpty() throws Exception {
        var filter = new JwtAuthenticationFilter(new FailingTokenProviderPort());
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid-token");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private record StubTokenProviderPort(AuthenticatedUserPayload payload) implements TokenProviderPort {
        @Override
        public AuthToken issue(co.edu.uniquindio.triage.domain.model.User user) {
            throw new UnsupportedOperationException("Not needed in this test");
        }

        @Override
        public AuthenticatedUserPayload parse(String token) {
            return payload;
        }
    }

    private static final class FailingTokenProviderPort implements TokenProviderPort {
        @Override
        public AuthToken issue(co.edu.uniquindio.triage.domain.model.User user) {
            throw new UnsupportedOperationException("Not needed in this test");
        }

        @Override
        public AuthenticatedUserPayload parse(String token) {
            throw new IllegalArgumentException("Invalid token");
        }
    }
}
