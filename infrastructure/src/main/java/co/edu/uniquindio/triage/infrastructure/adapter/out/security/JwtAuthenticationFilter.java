package co.edu.uniquindio.triage.infrastructure.adapter.out.security;

import co.edu.uniquindio.triage.application.port.out.persistence.LoadUserAuthPort;
import co.edu.uniquindio.triage.application.port.out.security.AuthenticatedUserPayload;
import co.edu.uniquindio.triage.application.port.out.security.TokenProviderPort;
import co.edu.uniquindio.triage.domain.model.id.UserId;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
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
            var path = request.getMethod() + " " + request.getRequestURI();
            AuthenticatedUserPayload payload = null;
            try {
                payload = tokenProviderPort.parse(token);
                var user = loadUserAuthPort.loadById(UserId.of(payload.userId()));

                if (user.isEmpty()) {
                    log.warn("JWT rejected — user not found: uid={} subject={} path={}",
                            payload.userId(), payload.username(), path);
                    SecurityContextHolder.clearContext();
                } else if (!user.get().isActive()) {
                    log.warn("JWT rejected — user inactive: uid={} subject={} path={}",
                            payload.userId(), payload.username(), path);
                    SecurityContextHolder.clearContext();
                } else {
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
                }
            } catch (ExpiredJwtException exception) {
                log.warn("JWT rejected — expired: subject={} expired_at={} path={}",
                        exception.getClaims() != null ? exception.getClaims().getSubject() : "<unknown>",
                        exception.getClaims() != null ? exception.getClaims().getExpiration() : "<unknown>",
                        path);
                SecurityContextHolder.clearContext();
            } catch (SignatureException exception) {
                log.warn("JWT rejected — signature mismatch (secret rotated or token tampered): path={} msg={}",
                        path, exception.getMessage());
                SecurityContextHolder.clearContext();
            } catch (MalformedJwtException | UnsupportedJwtException exception) {
                log.warn("JWT rejected — {}: path={} msg={}",
                        exception.getClass().getSimpleName(), path, exception.getMessage());
                SecurityContextHolder.clearContext();
            } catch (RuntimeException exception) {
                log.warn("JWT rejected — unexpected {}: uid={} path={} msg={}",
                        exception.getClass().getSimpleName(),
                        payload != null ? payload.userId() : "<pre-parse>",
                        path, exception.getMessage());
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
