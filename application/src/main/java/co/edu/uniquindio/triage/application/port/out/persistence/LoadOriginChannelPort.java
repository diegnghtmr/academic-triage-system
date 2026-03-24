package co.edu.uniquindio.triage.application.port.out.persistence;

import co.edu.uniquindio.triage.domain.model.OriginChannel;
import co.edu.uniquindio.triage.domain.model.id.OriginChannelId;

import java.util.Optional;

public interface LoadOriginChannelPort {

    Optional<OriginChannel> loadById(OriginChannelId originChannelId);
}
