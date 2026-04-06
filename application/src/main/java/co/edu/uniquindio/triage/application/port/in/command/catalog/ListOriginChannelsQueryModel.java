package co.edu.uniquindio.triage.application.port.in.command.catalog;

import java.util.Optional;

public record ListOriginChannelsQueryModel(Optional<Boolean> active) {

    public ListOriginChannelsQueryModel {
        active = active == null || active.isEmpty() ? Optional.of(Boolean.TRUE) : active;
    }
}
