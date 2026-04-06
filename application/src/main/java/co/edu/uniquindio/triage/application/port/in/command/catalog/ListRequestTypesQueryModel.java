package co.edu.uniquindio.triage.application.port.in.command.catalog;

import java.util.Optional;

public record ListRequestTypesQueryModel(Optional<Boolean> active) {

    public ListRequestTypesQueryModel {
        active = active == null || active.isEmpty() ? Optional.of(Boolean.TRUE) : active;
    }
}
