package co.edu.uniquindio.triage.application.service.user;

import co.edu.uniquindio.triage.application.port.in.user.GetUserVersionUseCase;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadUserVersionPort;
import co.edu.uniquindio.triage.domain.model.id.UserId;

import java.util.Objects;
import java.util.Optional;

public class GetUserVersionService implements GetUserVersionUseCase {

    private final LoadUserVersionPort loadUserVersionPort;

    public GetUserVersionService(LoadUserVersionPort loadUserVersionPort) {
        this.loadUserVersionPort = Objects.requireNonNull(loadUserVersionPort,
                "loadUserVersionPort no puede ser null");
    }

    @Override
    public Optional<Long> getVersionById(UserId id) {
        Objects.requireNonNull(id, "El id no puede ser null");
        return loadUserVersionPort.findVersionById(id);
    }
}
