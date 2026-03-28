package co.edu.uniquindio.triage.application.service.catalog;

import co.edu.uniquindio.triage.application.port.in.auth.AuthenticatedActor;
import co.edu.uniquindio.triage.application.port.in.command.catalog.CreateOriginChannelCommand;
import co.edu.uniquindio.triage.application.port.in.command.catalog.GetOriginChannelQueryModel;
import co.edu.uniquindio.triage.application.port.in.command.catalog.ListOriginChannelsQueryModel;
import co.edu.uniquindio.triage.application.port.in.command.catalog.UpdateOriginChannelCommand;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadOriginChannelPort;
import co.edu.uniquindio.triage.application.port.out.persistence.SaveOriginChannelPort;
import co.edu.uniquindio.triage.domain.enums.Role;
import co.edu.uniquindio.triage.domain.exception.DuplicateCatalogEntryException;
import co.edu.uniquindio.triage.domain.exception.EntityNotFoundException;
import co.edu.uniquindio.triage.domain.exception.UnauthorizedOperationException;
import co.edu.uniquindio.triage.domain.model.OriginChannel;
import co.edu.uniquindio.triage.domain.model.id.OriginChannelId;
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

class OriginChannelCatalogServicesTest {

    private static final AuthenticatedActor ADMIN = new AuthenticatedActor(new UserId(1L), "admin", Role.ADMIN);
    private static final AuthenticatedActor STUDENT = new AuthenticatedActor(new UserId(3L), "student", Role.STUDENT);

    private InMemoryOriginChannelCatalogPort catalogPort;
    private ListOriginChannelsService listService;
    private GetOriginChannelService getService;
    private CreateOriginChannelService createService;
    private UpdateOriginChannelService updateService;

    @BeforeEach
    void setUp() {
        catalogPort = new InMemoryOriginChannelCatalogPort();
        listService = new ListOriginChannelsService(catalogPort);
        getService = new GetOriginChannelService(catalogPort);
        createService = new CreateOriginChannelService(catalogPort, catalogPort);
        updateService = new UpdateOriginChannelService(catalogPort, catalogPort);
    }

    @Test
    void listMustDefaultToActiveTrueWhenFilterIsOmitted() {
        catalogPort.store(new OriginChannel(new OriginChannelId(2L), "Correo", true));
        catalogPort.store(new OriginChannel(new OriginChannelId(1L), "Teléfono", false));

        var result = listService.execute(new ListOriginChannelsQueryModel(Optional.empty()), STUDENT);

        assertThat(result)
                .extracting(OriginChannel::getName)
                .containsExactly("Correo");
        assertThat(catalogPort.lastActiveFilter()).contains(true);
    }

    @Test
    void getMustFailWhenOriginChannelDoesNotExist() {
        assertThatThrownBy(() -> getService.execute(new GetOriginChannelQueryModel(new OriginChannelId(99L)), STUDENT))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("OriginChannel");
    }

    @Test
    void adminCreateMustPersistActiveOriginChannel() {
        var created = createService.execute(new CreateOriginChannelCommand("  Portal web  "), ADMIN);

        assertThat(created.getId()).isEqualTo(new OriginChannelId(1L));
        assertThat(created.getName()).isEqualTo("Portal web");
        assertThat(created.isActive()).isTrue();
    }

    @Test
    void nonAdminWritesMustBeRejected() {
        assertThatThrownBy(() -> createService.execute(new CreateOriginChannelCommand("Portal"), STUDENT))
                .isInstanceOf(UnauthorizedOperationException.class);

        catalogPort.store(new OriginChannel(new OriginChannelId(4L), "Correo", true));

        assertThatThrownBy(() -> updateService.execute(
                new UpdateOriginChannelCommand(new OriginChannelId(4L), "Portal"),
                STUDENT
        )).isInstanceOf(UnauthorizedOperationException.class);
    }

    @Test
    void duplicateCreateMustBeRejectedBeforeSave() {
        catalogPort.store(new OriginChannel(new OriginChannelId(10L), "Correo", true));

        assertThatThrownBy(() -> createService.execute(new CreateOriginChannelCommand(" correo "), ADMIN))
                .isInstanceOf(DuplicateCatalogEntryException.class)
                .hasMessageContaining("canal de origen");
    }

    @Test
    void updateMustFailWhenOriginChannelDoesNotExist() {
        assertThatThrownBy(() -> updateService.execute(
                new UpdateOriginChannelCommand(new OriginChannelId(12L), "Portal"),
                ADMIN
        )).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void duplicateUpdateMustBeRejected() {
        catalogPort.store(new OriginChannel(new OriginChannelId(1L), "Correo", true));
        catalogPort.store(new OriginChannel(new OriginChannelId(2L), "Portal", true));

        assertThatThrownBy(() -> updateService.execute(
                new UpdateOriginChannelCommand(new OriginChannelId(2L), " correo "),
                ADMIN
        )).isInstanceOf(DuplicateCatalogEntryException.class);
    }

    @Test
    void adminUpdateMustReplaceNameAndPreserveActiveStatus() {
        catalogPort.store(new OriginChannel(new OriginChannelId(5L), "Correo", false));

        var updated = updateService.execute(
                new UpdateOriginChannelCommand(new OriginChannelId(5L), "Portal"),
                ADMIN
        );

        assertThat(updated.getName()).isEqualTo("Portal");
        assertThat(updated.isActive()).isFalse();
        assertThat(catalogPort.loadById(new OriginChannelId(5L))).contains(updated);
    }

    private static final class InMemoryOriginChannelCatalogPort implements LoadOriginChannelPort, SaveOriginChannelPort {

        private final AtomicLong sequence = new AtomicLong(1);
        private final Map<Long, OriginChannel> originChannels = new HashMap<>();
        private Optional<Boolean> lastActiveFilter = Optional.empty();

        @Override
        public Optional<OriginChannel> loadById(OriginChannelId originChannelId) {
            return Optional.ofNullable(originChannels.get(originChannelId.value()));
        }

        @Override
        public List<OriginChannel> loadAllOriginChannels(Optional<Boolean> active) {
            lastActiveFilter = active;
            var values = new ArrayList<>(originChannels.values());
            return values.stream()
                    .filter(originChannel -> active.map(flag -> originChannel.isActive() == flag).orElse(true))
                    .sorted(Comparator.comparing(OriginChannel::getName))
                    .toList();
        }

        @Override
        public boolean existsOriginChannelByNameIgnoreCase(String name) {
            return originChannels.values().stream().anyMatch(originChannel -> originChannel.getName().equalsIgnoreCase(name));
        }

        @Override
        public boolean existsOriginChannelByNameIgnoreCaseAndIdNot(String name, OriginChannelId originChannelId) {
            return originChannels.values().stream()
                    .anyMatch(originChannel -> originChannel.getName().equalsIgnoreCase(name)
                            && !originChannel.getId().equals(originChannelId));
        }

        @Override
        public OriginChannel saveOriginChannel(OriginChannel originChannel) {
            if (originChannel.getId() == null) {
                var persisted = new OriginChannel(new OriginChannelId(sequence.getAndIncrement()), originChannel.getName(), originChannel.isActive());
                originChannels.put(persisted.getId().value(), persisted);
                return persisted;
            }

            originChannels.put(originChannel.getId().value(), originChannel);
            return originChannel;
        }

        void store(OriginChannel originChannel) {
            originChannels.put(originChannel.getId().value(), originChannel);
            sequence.updateAndGet(current -> Math.max(current, originChannel.getId().value() + 1));
        }

        Optional<Boolean> lastActiveFilter() {
            return lastActiveFilter;
        }
    }
}
