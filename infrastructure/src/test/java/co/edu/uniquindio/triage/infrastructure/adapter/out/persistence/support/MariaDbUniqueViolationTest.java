package co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.support;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

class MariaDbUniqueViolationTest {

    @Test
    void duplicateKeyExceptionIsUniqueViolation() {
        var ex = new DuplicateKeyException("dup", null);
        assertThat(MariaDbUniqueViolation.isUniqueViolation(ex)).isTrue();
    }

    @Test
    void sqlError1062IsUniqueViolation() {
        var sql = new SQLException("Duplicate entry 'x' for key 'y'", "23000", 1062);
        var ex = new DataIntegrityViolationException("wrap", sql);
        assertThat(MariaDbUniqueViolation.isUniqueViolation(ex)).isTrue();
    }

    @Test
    void foreignKeyViolation1452IsNotTreatedAsUnique() {
        var sql = new SQLException("Cannot add or update a child row", "23000", 1452);
        var ex = new DataIntegrityViolationException("wrap", sql);
        assertThat(MariaDbUniqueViolation.isUniqueViolation(ex)).isFalse();
    }

    @Test
    void parsesDuplicateEntryMessage() {
        var sql = new SQLException(
                "Duplicate entry 'jperez' for key 'users.username'", "23000", 1062);
        var ex = new DataIntegrityViolationException("wrap", sql);
        var parsed = MariaDbUniqueViolation.parseDuplicateEntry(ex);
        assertThat(parsed).isPresent();
        assertThat(parsed.get().value()).isEqualTo("jperez");
        assertThat(parsed.get().indexName()).contains("username");
    }
}
