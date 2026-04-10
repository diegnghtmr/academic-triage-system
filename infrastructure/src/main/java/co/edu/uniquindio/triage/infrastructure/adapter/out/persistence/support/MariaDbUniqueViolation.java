package co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.support;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;

import java.sql.SQLException;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Detección conservadora de violaciones de unicidad en MariaDB/MySQL (código 1062).
 * No trata otras violaciones de integridad como duplicados.
 */
public final class MariaDbUniqueViolation {

    private static final Pattern DUPLICATE_ENTRY =
            Pattern.compile("Duplicate entry '([^']*)' for key '([^']*)'");

    private MariaDbUniqueViolation() {}

    public static boolean isUniqueViolation(DataIntegrityViolationException ex) {
        if (ex instanceof DuplicateKeyException) {
            return true;
        }
        for (Throwable t = ex.getCause(); t != null; t = t.getCause()) {
            if (t instanceof SQLException sql && sql.getErrorCode() == 1062) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param value valor duplicado
     * @param indexName nombre del índice o restricción en el mensaje del driver
     */
    public record DuplicateKeyDetails(String value, String indexName) {}

    public static Optional<DuplicateKeyDetails> parseDuplicateEntry(DataIntegrityViolationException ex) {
        var message = deepestSqlMessage(ex);
        if (message == null || !message.contains("Duplicate entry")) {
            return Optional.empty();
        }
        var m = DUPLICATE_ENTRY.matcher(message);
        if (!m.find()) {
            return Optional.empty();
        }
        return Optional.of(new DuplicateKeyDetails(m.group(1), m.group(2)));
    }

    private static String deepestSqlMessage(DataIntegrityViolationException ex) {
        String last = null;
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (t instanceof SQLException sql && sql.getMessage() != null) {
                last = sql.getMessage();
            } else if (t.getMessage() != null) {
                last = t.getMessage();
            }
        }
        return last;
    }
}
