package co.edu.uniquindio.triage.application.service.user;

import co.edu.uniquindio.triage.application.port.in.auth.AuthenticatedActor;
import co.edu.uniquindio.triage.application.port.in.user.UpdateUserUseCase;
import co.edu.uniquindio.triage.application.port.in.user.command.UpdateUserCommand;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadUserAuthPort;
import co.edu.uniquindio.triage.application.port.out.persistence.SaveUserPort;
import co.edu.uniquindio.triage.domain.exception.BusinessRuleViolationException;
import co.edu.uniquindio.triage.domain.exception.DuplicateUserException;
import co.edu.uniquindio.triage.domain.exception.EntityNotFoundException;
import co.edu.uniquindio.triage.domain.model.User;

import java.util.Objects;

public class UpdateUserService implements UpdateUserUseCase {

    private final LoadUserAuthPort loadUserAuthPort;
    private final SaveUserPort saveUserPort;

    public UpdateUserService(LoadUserAuthPort loadUserAuthPort, SaveUserPort saveUserPort) {
        this.loadUserAuthPort = Objects.requireNonNull(loadUserAuthPort);
        this.saveUserPort = Objects.requireNonNull(saveUserPort);
    }

    @Override
    public User execute(UpdateUserCommand command, AuthenticatedActor actor) {
        Objects.requireNonNull(actor, "El actor autenticado no puede ser null");

        var user = loadUserAuthPort.loadById(command.id())
                .orElseThrow(() -> new EntityNotFoundException("Usuario", "id", command.id().value()));

        if (isSelfModification(actor, user)) {
            guardSelfRoleChange(user, command);
            guardSelfDeactivation(user, command);
        }

        if (!user.getEmail().equals(command.email()) && loadUserAuthPort.existsByEmail(command.email())) {
            throw new DuplicateUserException("email", command.email().value());
        }

        if (!user.getIdentification().equals(command.identification()) &&
            loadUserAuthPort.existsByIdentification(command.identification())) {
            throw new DuplicateUserException("identificación", command.identification().value());
        }

        user.updateProfile(command.firstName(), command.lastName(), command.identification());
        user.updateEmail(command.email());
        user.updateRole(command.role());

        if (command.active()) {
            user.activate();
        } else {
            user.deactivate();
        }

        return saveUserPort.save(user);
    }

    private boolean isSelfModification(AuthenticatedActor actor, User user) {
        return user.getId()
                .map(id -> id.equals(actor.userId()))
                .orElse(false);
    }

    private void guardSelfRoleChange(User user, UpdateUserCommand command) {
        if (!user.getRole().equals(command.role())) {
            throw new BusinessRuleViolationException(
                    "No puede modificar su propio rol");
        }
    }

    private void guardSelfDeactivation(User user, UpdateUserCommand command) {
        if (user.isActive() && !command.active()) {
            throw new BusinessRuleViolationException(
                    "No puede desactivar su propia cuenta");
        }
    }
}
