package co.edu.uniquindio.triage.application.port.in.command.catalog;

import co.edu.uniquindio.triage.domain.model.id.OriginChannelId;
import co.edu.uniquindio.triage.domain.model.id.RequestTypeId;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CatalogCommandTest {

    @Test
    void listRequestTypesQueryMustDefaultActiveFilterToTrueWhenMissing() {
        var query = new ListRequestTypesQueryModel(null);

        assertThat(query.active()).contains(true);
    }

    @Test
    void listOriginChannelsQueryMustPreserveExplicitFalseFilter() {
        var query = new ListOriginChannelsQueryModel(Optional.of(false));

        assertThat(query.active()).contains(false);
    }

    @Test
    void createRequestTypeCommandMustTrimOptionalDescriptionAndNormalizeBlankToNull() {
        var commandWithDescription = new CreateRequestTypeCommand("  Homologación  ", "  Trámite con soportes  ");
        var commandWithoutDescription = new CreateRequestTypeCommand("Homologación", "   ");

        assertThat(commandWithDescription.name()).isEqualTo("Homologación");
        assertThat(commandWithDescription.description()).isEqualTo("Trámite con soportes");
        assertThat(commandWithoutDescription.description()).isNull();
    }

    @Test
    void createRequestTypeCommandMustRejectNameLongerThanOpenApiLimit() {
        assertThatThrownBy(() -> new CreateRequestTypeCommand("x".repeat(101), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El nombre no puede tener más de 100 caracteres");
    }

    @Test
    void updateRequestTypeCommandMustRequireIdentifier() {
        assertThatThrownBy(() -> new UpdateRequestTypeCommand(null, "Registro", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("El requestTypeId no puede ser null");
    }

    @Test
    void updateRequestTypeCommandMustTrimFields() {
        var command = new UpdateRequestTypeCommand(new RequestTypeId(5L), "  Reingreso  ", "  Cambio de cohorte  ");

        assertThat(command.name()).isEqualTo("Reingreso");
        assertThat(command.description()).isEqualTo("Cambio de cohorte");
    }

    @Test
    void createOriginChannelCommandMustTrimName() {
        var command = new CreateOriginChannelCommand("  Ventanilla  ");

        assertThat(command.name()).isEqualTo("Ventanilla");
    }

    @Test
    void updateOriginChannelCommandMustRequireIdentifier() {
        assertThatThrownBy(() -> new UpdateOriginChannelCommand(null, "Correo"))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("El originChannelId no puede ser null");
    }

    @Test
    void updateOriginChannelCommandMustRejectBlankName() {
        assertThatThrownBy(() -> new UpdateOriginChannelCommand(new OriginChannelId(3L), "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El nombre no puede ser null o vacío");
    }

    @Test
    void getCatalogQueriesMustRequireIdentifiers() {
        assertThatThrownBy(() -> new GetRequestTypeQueryModel(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("El requestTypeId no puede ser null");

        assertThatThrownBy(() -> new GetOriginChannelQueryModel(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("El originChannelId no puede ser null");
    }
}
