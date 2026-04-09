package co.edu.uniquindio.triage.infrastructure.adapter.out.security;

import co.edu.uniquindio.triage.application.port.out.persistence.LoadUserAuthPort;
import co.edu.uniquindio.triage.application.port.out.security.TokenProviderPort;
import co.edu.uniquindio.triage.domain.model.id.UserId;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final TokenProviderPort tokenProviderPort;
    private final LoadUserAuthPort loadUserAuthPort;

    public JwtAuthenticationFilter(TokenProviderPort tokenProviderPort, LoadUserAuthPort loadUserAuthPort) {
        this.tokenProviderPort = Objects.requireNonNull(tokenProviderPort);
        this.loadUserAuthPort = Objects.requireNonNull(loadUserAuthPort);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        var authorization = request.getHeader("Authorization");

        if (authorization != null && authorization.startsWith(BEARER_PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            var token = authorization.substring(BEARER_PREFIX.length());
            try {
                var payload = tokenProviderPort.parse(token);
                var user = loadUserAuthPort.loadById(UserId.of(payload.userId()));

                if (user.isPresent() && user.get().isActive()) {
                    var domainUser = user.get();
                    var principal = new AuthenticatedUser(
                            payload.userId(),
                            domainUser.getUsername().value(),
                            domainUser.getRole(),
                            domainUser.isActive()
                    );
                    var authentication = new UsernamePasswordAuthenticationToken(
                            principal,
                            token,
                            principal.getAuthorities()
                    );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else {
                    SecurityContextHolder.clearContext();
                }
            } catch (RuntimeException exception) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
