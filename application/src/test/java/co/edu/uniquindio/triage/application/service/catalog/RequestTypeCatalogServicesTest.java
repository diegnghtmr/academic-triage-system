package co.edu.uniquindio.triage.application.service.catalog;

import co.edu.uniquindio.triage.application.port.in.auth.AuthenticatedActor;
import co.edu.uniquindio.triage.application.port.in.command.catalog.CreateRequestTypeCommand;
import co.edu.uniquindio.triage.application.port.in.command.catalog.GetRequestTypeQueryModel;
import co.edu.uniquindio.triage.application.port.in.command.catalog.ListRequestTypesQueryModel;
import co.edu.uniquindio.triage.application.port.in.command.catalog.UpdateRequestTypeCommand;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadRequestTypePort;
import co.edu.uniquindio.triage.application.port.out.persistence.SaveRequestTypePort;
import co.edu.uniquindio.triage.domain.enums.Role;
import co.edu.uniquindio.triage.domain.exception.DuplicateCatalogEntryException;
import co.edu.uniquindio.triage.domain.exception.EntityNotFoundException;
import co.edu.uniquindio.triage.domain.exception.UnauthorizedOperationException;
import co.edu.uniquindio.triage.domain.model.RequestType;
import co.edu.uniquindio.triage.domain.model.id.RequestTypeId;
import co.edu.uniquindio.triage.domain.model.id.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequestTypeCatalogServicesTest {

    private static final AuthenticatedActor ADMIN = new AuthenticatedActor(new UserId(1L), "admin", Role.ADMIN);
    private static final AuthenticatedActor STAFF = new AuthenticatedActor(new UserId(2L), "staff", Role.STAFF);

    private InMemoryRequestTypeCatalogPort catalogPort;
    private ListRequestTypesService listService;
    private GetRequestTypeService getService;
    private CreateRequestTypeService createService;
    private UpdateRequestTypeService updateService;

    @BeforeEach
    void setUp() {
        catalogPort = new InMemoryRequestTypeCatalogPort();
        listService = new ListRequestTypesService(catalogPort);
        getService = new GetRequestTypeService(catalogPort);
        createService = new CreateRequestTypeService(catalogPort, catalogPort);
        updateService = new UpdateRequestTypeService(catalogPort, catalogPort);
    }

    @Test
    void listMustDefaultToActiveTrueForAuthenticatedReads() {
        catalogPort.store(new RequestType(new RequestTypeId(2L), "Homologación", "Cambio", false));
        catalogPort.store(new RequestType(new RequestTypeId(1L), "Cupo", "Solicitud", true));

        var result = listService.execute(new ListRequestTypesQueryModel(Optional.empty()), STAFF);

        assertThat(result)
                .extracting(RequestType::getName)
                .containsExactly("Cupo");
        assertThat(catalogPort.lastActiveFilter()).contains(true);
    }

    @Test
    void getMustFailWhenRequestTypeDoesNotExist() {
        assertThatThrownBy(() -> getService.execute(new GetRequestTypeQueryModel(new RequestTypeId(99L)), STAFF))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("RequestType");
    }

    @Test
    void adminCreateMustPersistActiveRequestType() {
        var created = createService.execute(new CreateRequestTypeCommand("  Cupo especial  ", "  Solicitud prioritaria  "), ADMIN);

        assertThat(created.getId()).isEqualTo(new RequestTypeId(1L));
        assertThat(created.getName()).isEqualTo("Cupo especial");
        assertThat(created.getDescription()).isEqualTo("Solicitud prioritaria");
        assertThat(created.isActive()).isTrue();
        assertThat(catalogPort.loadById(created.getId())).contains(created);
    }

    @Test
    void nonAdminCreateMustBeRejected() {
        assertThatThrownBy(() -> createService.execute(new CreateRequestTypeCommand("Cupo", "Solicitud"), STAFF))
                .isInstanceOf(UnauthorizedOperationException.class);
    }

    @Test
    void duplicateCreateMustBeRejectedBeforeSave() {
        catalogPort.store(new RequestType(new RequestTypeId(7L), "Cupo", "Solicitud", true));

        assertThatThrownBy(() -> createService.execute(new CreateRequestTypeCommand("  cupo  ", "Otra"), ADMIN))
                .isInstanceOf(DuplicateCatalogEntryException.class)
                .hasMessageContaining("tipo de solicitud");

        assertThat(catalogPort.storeSize()).isEqualTo(1);
    }

    @Test
    void updateMustFailWhenRequestTypeDoesNotExist() {
        assertThatThrownBy(() -> updateService.execute(
                new UpdateRequestTypeCommand(new RequestTypeId(20L), "Nuevo", "Descripción"),
                ADMIN
        )).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void duplicateUpdateMustBeRejected() {
        catalogPort.store(new RequestType(new RequestTypeId(1L), "Cupo", "Solicitud", true));
        catalogPort.store(new RequestType(new RequestTypeId(2L), "Homologación", "Cambio", true));

        assertThatThrownBy(() -> updateService.execute(
                new UpdateRequestTypeCommand(new RequestTypeId(2L), "  cupo  ", "Cambio"),
                ADMIN
        )).isInstanceOf(DuplicateCatalogEntryException.class);
    }

    @Test
    void adminUpdateMustReplaceEditableFieldsAndPreserveActiveStatus() {
        catalogPort.store(new RequestType(new RequestTypeId(3L), "Cupo", "Solicitud", false));

        var updated = updateService.execute(
                new UpdateRequestTypeCommand(new RequestTypeId(3L), "Reintegro", "Actualizada"),
                ADMIN
        );

        assertThat(updated.getName()).isEqualTo("Reintegro");
        assertThat(updated.getDescription()).isEqualTo("Actualizada");
        assertThat(updated.isActive()).isFalse();
        assertThat(catalogPort.loadById(new RequestTypeId(3L))).contains(updated);
    }

    private static final class InMemoryRequestTypeCatalogPort implements LoadRequestTypePort, SaveRequestTypePort {

        private final AtomicLong sequence = new AtomicLong(1);
        private final Map<Long, RequestType> requestTypes = new HashMap<>();
        private Optional<Boolean> lastActiveFilter = Optional.empty();

        @Override
        public Optional<RequestType> loadById(RequestTypeId requestTypeId) {
            return Optional.ofNullable(requestTypes.get(requestTypeId.value()));
        }

        @Override
        public List<RequestType> loadAllRequestTypes(Optional<Boolean> active) {
            lastActiveFilter = active;
            var values = new ArrayList<>(requestTypes.values());
            return values.stream()
                    .filter(requestType -> active.map(flag -> requestType.isActive() == flag).orElse(true))
                    .sorted(Comparator.comparing(RequestType::getName))
                    .toList();
        }

        @Override
        public boolean existsRequestTypeByNameIgnoreCase(String name) {
            return requestTypes.values().stream().anyMatch(requestType -> requestType.getName().equalsIgnoreCase(name));
        }

        @Override
        public boolean existsRequestTypeByNameIgnoreCaseAndIdNot(String name, RequestTypeId requestTypeId) {
            return requestTypes.values().stream()
                    .anyMatch(requestType -> requestType.getName().equalsIgnoreCase(name)
                            && !requestType.getId().equals(requestTypeId));
        }

        @Override
        public RequestType saveRequestType(RequestType requestType) {
            if (requestType.getId() == null) {
                var persisted = new RequestType(new RequestTypeId(sequence.getAndIncrement()), requestType.getName(), requestType.getDescription(), requestType.isActive());
                requestTypes.put(persisted.getId().value(), persisted);
                return persisted;
            }

            requestTypes.put(requestType.getId().value(), requestType);
            return requestType;
        }

        void store(RequestType requestType) {
            requestTypes.put(requestType.getId().value(), requestType);
            sequence.updateAndGet(current -> Math.max(current, requestType.getId().value() + 1));
        }

        Optional<Boolean> lastActiveFilter() {
            return lastActiveFilter;
        }

        int storeSize() {
            return requestTypes.size();
        }
    }
}
