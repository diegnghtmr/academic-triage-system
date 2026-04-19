package co.edu.uniquindio.triage.application.service.businessrule;

import co.edu.uniquindio.triage.application.port.in.businessrule.GetBusinessRuleVersionUseCase;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadBusinessRuleVersionPort;
import co.edu.uniquindio.triage.domain.model.id.BusinessRuleId;

import java.util.Objects;
import java.util.Optional;

public class GetBusinessRuleVersionService implements GetBusinessRuleVersionUseCase {

    private final LoadBusinessRuleVersionPort loadBusinessRuleVersionPort;

    public GetBusinessRuleVersionService(LoadBusinessRuleVersionPort loadBusinessRuleVersionPort) {
        this.loadBusinessRuleVersionPort = Objects.requireNonNull(loadBusinessRuleVersionPort,
                "loadBusinessRuleVersionPort no puede ser null");
    }

    @Override
    public Optional<Long> getVersionById(BusinessRuleId id) {
        Objects.requireNonNull(id, "El id no puede ser null");
        return loadBusinessRuleVersionPort.findVersionById(id);
    }
}
