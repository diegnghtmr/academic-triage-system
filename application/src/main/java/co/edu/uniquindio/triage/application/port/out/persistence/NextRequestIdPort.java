package co.edu.uniquindio.triage.application.port.out.persistence;

import co.edu.uniquindio.triage.domain.model.id.RequestId;

public interface NextRequestIdPort {

    RequestId nextId();
}
