package co.edu.uniquindio.triage.application.port.out.persistence;

import co.edu.uniquindio.triage.domain.model.OriginChannel;
import co.edu.uniquindio.triage.domain.model.id.OriginChannelId;

import java.util.List;
import java.util.Optional;

public interface LoadOriginChannelPort {

    String UNSUPPORTED_OPERATION_MESSAGE = "La operación de administración de canales de origen aún no está implementada";

    Optional<OriginChannel> loadById(OriginChannelId originChannelId);

    default List<OriginChannel> loadAllOriginChannels(Optional<Boolean> active) {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    default boolean existsOriginChannelByNameIgnoreCase(String name) {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    default boolean existsOriginChannelByNameIgnoreCaseAndIdNot(String name, OriginChannelId originChannelId) {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }
}
