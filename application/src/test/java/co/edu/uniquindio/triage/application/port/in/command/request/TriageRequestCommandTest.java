package co.edu.uniquindio.triage.application.port.in.command.request;

import co.edu.uniquindio.triage.domain.enums.Priority;
import co.edu.uniquindio.triage.domain.model.id.RequestId;
import co.edu.uniquindio.triage.domain.model.id.RequestTypeId;
import co.edu.uniquindio.triage.domain.model.id.UserId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TriageRequestCommandTest {

    @Test
    void classifyCommandMustTrimOptionalObservations() {
        var command = new ClassifyRequestCommand(new RequestId(10L), new RequestTypeId(4L), "  Reclasificada por coordinación.  ");

        assertThat(command.observations()).isEqualTo("Reclasificada por coordinación.");
    }

    @Test
    void classifyCommandMustNormalizeBlankObservationsToNull() {
        var command = new ClassifyRequestCommand(new RequestId(10L), new RequestTypeId(4L), "   ");

        assertThat(command.observations()).isNull();
    }

    @Test
    void prioritizeCommandMustTrimAndRequireJustificationLength() {
        var command = new PrioritizeRequestCommand(new RequestId(11L), Priority.HIGH, "   Alta urgencia académica   ");

        assertThat(command.justification()).isEqualTo("Alta urgencia académica");
    }

    @Test
    void prioritizeCommandMustRejectBlankJustification() {
        assertThatThrownBy(() -> new PrioritizeRequestCommand(new RequestId(11L), Priority.HIGH, "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La justificación no puede ser null o vacía");
    }

    @Test
    void assignCommandMustTrimOptionalObservations() {
        var command = new AssignRequestCommand(new RequestId(12L), new UserId(8L), "  Asignada al coordinador de registro.  ");

        assertThat(command.observations()).isEqualTo("Asignada al coordinador de registro.");
    }

    @Test
    void assignCommandMustRejectObservationsLongerThanOpenApiLimit() {
        var observations = "a".repeat(1001);

        assertThatThrownBy(() -> new AssignRequestCommand(new RequestId(12L), new UserId(8L), observations))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Las observaciones no pueden tener más de 1000 caracteres");
    }
}
