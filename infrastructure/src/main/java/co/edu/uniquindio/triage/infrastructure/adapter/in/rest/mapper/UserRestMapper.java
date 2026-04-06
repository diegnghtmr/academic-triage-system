package co.edu.uniquindio.triage.infrastructure.adapter.in.rest.mapper;

import co.edu.uniquindio.triage.application.port.in.user.command.UpdateUserCommand;
import co.edu.uniquindio.triage.domain.model.Email;
import co.edu.uniquindio.triage.domain.model.Identification;
import co.edu.uniquindio.triage.domain.model.User;
import co.edu.uniquindio.triage.domain.model.id.UserId;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.user.UpdateUserRequest;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.user.UserResponse;
import org.springframework.stereotype.Component;

@Component
public class UserRestMapper {

    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId().orElseThrow().value(),
                user.getUsername().value(),
                user.getFirstName(),
                user.getLastName(),
                user.getIdentification().value(),
                user.getEmail().value(),
                user.getRole().name(),
                user.isActive()
        );
    }

    public UpdateUserCommand toCommand(UserId id, UpdateUserRequest request) {
        return new UpdateUserCommand(
                id,
                request.firstName(),
                request.lastName(),
                new Identification(request.identification()),
                new Email(request.email()),
                request.role(),
                request.active()
        );
    }
}
