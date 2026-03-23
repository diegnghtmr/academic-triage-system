/**
 * Shared request-use-case seams. Future request lifecycle ports must accept the application-level
 * authenticated actor instead of Spring Security types. Rehydrate the canonical {@code User}
 * through persistence ports only when current mutable user state is required by domain rules.
 */
package co.edu.uniquindio.triage.application.port.in.request;
