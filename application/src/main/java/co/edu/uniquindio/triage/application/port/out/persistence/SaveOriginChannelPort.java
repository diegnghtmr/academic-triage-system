package co.edu.uniquindio.triage.application.port.out.persistence;

import co.edu.uniquindio.triage.domain.model.OriginChannel;

public interface SaveOriginChannelPort {

    OriginChannel saveOriginChannel(OriginChannel originChannel);
}
