package co.edu.uniquindio.triage.infrastructure.adapter.in.rest.support;

import co.edu.uniquindio.triage.application.exception.ETagMismatchException;
import co.edu.uniquindio.triage.application.exception.MissingIfMatchPreconditionException;
import org.springframework.stereotype.Component;

@Component
public class ETagSupport {

    public String toETagValue(long version) {
        return "\"" + version + "\"";
    }

    public long parseIfMatch(String ifMatchHeader) {
        if (ifMatchHeader == null || ifMatchHeader.isBlank()) {
            throw new MissingIfMatchPreconditionException();
        }
        String stripped = ifMatchHeader.strip();
        // Weak ETags are not accepted: optimistic-lock semantics require strong ETags only
        if (stripped.startsWith("W/")) {
            throw new ETagMismatchException();
        }
        // Enforce strict strong-ETag format: "N" where N is a long
        if (!stripped.startsWith("\"") || !stripped.endsWith("\"") || stripped.length() < 3) {
            throw new ETagMismatchException();
        }
        String inner = stripped.substring(1, stripped.length() - 1);
        try {
            return Long.parseLong(inner);
        } catch (NumberFormatException e) {
            throw new ETagMismatchException();
        }
    }
}
