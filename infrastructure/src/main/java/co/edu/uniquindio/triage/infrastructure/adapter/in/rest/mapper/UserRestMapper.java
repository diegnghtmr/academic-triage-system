package co.edu.uniquindio.triage.infrastructure.adapter.in.rest.mapper;

import co.edu.uniquindio.triage.domain.model.User;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.user.UserResponse;

public class UserRestMapper {

    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId() != null ? user.getId().value() : null,
                user.getUsername().value(),
                user.getEmail().value(),
                user.getFirstName(),
                user.getLastName(),
                user.getIdentification().value(),
                user.getRole(),
                user.isActive()
        );
    }
}
