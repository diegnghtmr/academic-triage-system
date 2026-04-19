package co.edu.uniquindio.triage.infrastructure.adapter.in.rest;

import co.edu.uniquindio.triage.application.exception.ETagMismatchException;
import co.edu.uniquindio.triage.application.port.in.common.Page;
import co.edu.uniquindio.triage.application.port.in.user.GetUserByIdQuery;
import co.edu.uniquindio.triage.application.port.in.user.GetUserVersionUseCase;
import co.edu.uniquindio.triage.application.port.in.user.GetUsersQuery;
import co.edu.uniquindio.triage.application.port.in.user.UpdateUserUseCase;
import co.edu.uniquindio.triage.application.port.in.user.command.GetUsersQueryModel;
import co.edu.uniquindio.triage.domain.enums.Role;
import co.edu.uniquindio.triage.domain.exception.EntityNotFoundException;
import co.edu.uniquindio.triage.domain.model.id.UserId;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.user.UpdateUserRequest;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.user.UserResponse;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.mapper.UserRestMapper;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.support.AuthenticatedActorMapper;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.support.ETagSupport;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/users")
class UserController {

    private final GetUsersQuery getUsersQuery;
    private final GetUserByIdQuery getUserByIdQuery;
    private final UpdateUserUseCase updateUserUseCase;
    private final GetUserVersionUseCase getUserVersionUseCase;
    private final UserRestMapper userRestMapper;
    private final AuthenticatedActorMapper authenticatedActorMapper;
    private final ETagSupport eTagSupport;

    public UserController(GetUsersQuery getUsersQuery,
                          GetUserByIdQuery getUserByIdQuery,
                          UpdateUserUseCase updateUserUseCase,
                          GetUserVersionUseCase getUserVersionUseCase,
                          UserRestMapper userRestMapper,
                          AuthenticatedActorMapper authenticatedActorMapper,
                          ETagSupport eTagSupport) {
        this.getUsersQuery = getUsersQuery;
        this.getUserByIdQuery = getUserByIdQuery;
        this.updateUserUseCase = updateUserUseCase;
        this.getUserVersionUseCase = getUserVersionUseCase;
        this.userRestMapper = userRestMapper;
        this.authenticatedActorMapper = authenticatedActorMapper;
        this.eTagSupport = eTagSupport;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Page<UserResponse> getUsers(
            @RequestParam(name = "role") Optional<Role> role,
            @RequestParam(name = "active") Optional<Boolean> active,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "sort", defaultValue = "username,asc") String sort
    ) {
        var query = new GetUsersQueryModel(role, active, page, size, sort);
        var usersPage = getUsersQuery.execute(query);

        return new Page<>(
                usersPage.content().stream().map(userRestMapper::toResponse).toList(),
                usersPage.totalElements(),
                usersPage.totalPages(),
                usersPage.currentPage(),
                usersPage.pageSize()
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'STUDENT')")
    public ResponseEntity<UserResponse> getUserById(@PathVariable("id") Long id,
                                                    Authentication authentication) {
        var actor = authenticatedActorMapper.toRequiredActor(authentication);
        var user = getUserByIdQuery.execute(new UserId(id), actor)
                .orElseThrow(() -> new EntityNotFoundException("User", "id", id));
        var response = userRestMapper.toResponse(user);
        var version = getUserVersionUseCase.getVersionById(new UserId(id)).orElse(null);
        if (version != null) {
            return ResponseEntity.ok()
                    .eTag(eTagSupport.toETagValue(version))
                    .body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable("id") Long id,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @Valid @RequestBody UpdateUserRequest request,
            Authentication authentication
    ) {
        long expectedVersion = eTagSupport.parseIfMatch(ifMatch);
        long currentVersion = getUserVersionUseCase.getVersionById(new UserId(id))
                .orElseThrow(() -> new EntityNotFoundException("User", "id", id));
        if (expectedVersion != currentVersion) {
            throw new ETagMismatchException();
        }
        var actor = authenticatedActorMapper.toRequiredActor(authentication);
        var command = userRestMapper.toCommand(new UserId(id), request);
        var updatedUser = updateUserUseCase.execute(command, actor);
        return ResponseEntity.ok(userRestMapper.toResponse(updatedUser));
    }
}
