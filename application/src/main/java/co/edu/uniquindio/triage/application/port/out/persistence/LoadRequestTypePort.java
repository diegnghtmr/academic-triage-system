package co.edu.uniquindio.triage.application.port.out.persistence;

import co.edu.uniquindio.triage.domain.model.RequestType;
import co.edu.uniquindio.triage.domain.model.id.RequestTypeId;

import java.util.List;
import java.util.Optional;

public interface LoadRequestTypePort {

    String UNSUPPORTED_OPERATION_MESSAGE = "La operación de administración de tipos de solicitud aún no está implementada";

    Optional<RequestType> loadById(RequestTypeId requestTypeId);

    default List<RequestType> loadAllRequestTypes(Optional<Boolean> active) {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    default boolean existsRequestTypeByNameIgnoreCase(String name) {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    default boolean existsRequestTypeByNameIgnoreCaseAndIdNot(String name, RequestTypeId requestTypeId) {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }
}
