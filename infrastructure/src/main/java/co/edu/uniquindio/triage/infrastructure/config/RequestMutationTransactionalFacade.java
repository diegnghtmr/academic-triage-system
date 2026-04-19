package co.edu.uniquindio.triage.infrastructure.config;

import co.edu.uniquindio.triage.application.port.in.auth.AuthenticatedActor;
import co.edu.uniquindio.triage.application.port.in.command.request.*;
import co.edu.uniquindio.triage.application.port.in.request.*;
import co.edu.uniquindio.triage.application.port.out.persistence.*;
import co.edu.uniquindio.triage.application.service.request.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Transactional boundary for all AcademicRequest mutation use cases.
 *
 * Each public method is a single @Transactional unit that spans:
 *   loadByIdForMutation() → acquires PESSIMISTIC_WRITE lock
 *   domain mutation (in memory)
 *   save() → stays within the same transaction
 *
 * The lock is held until this method commits, preventing concurrent mutations
 * from causing lost-update races or history duplication on the same request.
 *
 * This class does NOT implement the mutation use case interfaces directly because
 * all 7 interfaces extend AuthenticatedRequestUseCase<T, R> — a generic interface —
 * and Java's type erasure prevents a single class from inheriting the same raw type
 * with different type arguments. Instead, BeanConfiguration exposes @Bean lambdas
 * that delegate to this class, going through the Spring proxy so @Transactional applies.
 */
@Component
@Transactional
class RequestMutationTransactionalFacade implements AddInternalNoteUseCase {

    private final ClassifyRequestService classifyService;
    private final PrioritizeRequestService prioritizeService;
    private final AssignRequestService assignService;
    private final AttendRequestService attendService;
    private final CloseRequestService closeService;
    private final CancelRequestService cancelService;
    private final RejectRequestService rejectService;
    private final AddInternalNoteService addInternalNoteService;

    RequestMutationTransactionalFacade(
            LoadRequestForMutationPort loadRequestForMutationPort,
            LoadRequestPort loadRequestPort,
            LoadRequestTypePort loadRequestTypePort,
            LoadOriginChannelPort loadOriginChannelPort,
            LoadUserAuthPort loadUserAuthPort,
            SaveRequestPort saveRequestPort) {

        Objects.requireNonNull(loadRequestForMutationPort);
        Objects.requireNonNull(loadRequestPort);
        Objects.requireNonNull(loadRequestTypePort);
        Objects.requireNonNull(loadOriginChannelPort);
        Objects.requireNonNull(loadUserAuthPort);
        Objects.requireNonNull(saveRequestPort);

        this.classifyService = new ClassifyRequestService(
                loadRequestForMutationPort, loadRequestTypePort, loadOriginChannelPort, loadUserAuthPort, saveRequestPort);
        this.prioritizeService = new PrioritizeRequestService(
                loadRequestForMutationPort, loadRequestTypePort, loadOriginChannelPort, loadUserAuthPort, saveRequestPort);
        this.assignService = new AssignRequestService(
                loadRequestForMutationPort, loadRequestTypePort, loadOriginChannelPort, loadUserAuthPort, saveRequestPort);
        this.attendService = new AttendRequestService(
                loadRequestForMutationPort, loadRequestTypePort, loadOriginChannelPort, loadUserAuthPort, saveRequestPort);
        this.closeService = new CloseRequestService(
                loadRequestForMutationPort, loadRequestTypePort, loadOriginChannelPort, loadUserAuthPort, saveRequestPort);
        this.cancelService = new CancelRequestService(
                loadRequestForMutationPort, loadRequestTypePort, loadOriginChannelPort, loadUserAuthPort, saveRequestPort);
        this.rejectService = new RejectRequestService(
                loadRequestForMutationPort, loadRequestTypePort, loadOriginChannelPort, loadUserAuthPort, saveRequestPort);
        this.addInternalNoteService = new AddInternalNoteService(
                loadRequestForMutationPort, loadRequestPort, saveRequestPort);
    }

    RequestSummary classify(ClassifyRequestCommand command, AuthenticatedActor actor) {
        return classifyService.execute(command, actor);
    }

    RequestSummary prioritize(PrioritizeRequestCommand command, AuthenticatedActor actor) {
        return prioritizeService.execute(command, actor);
    }

    RequestSummary assign(AssignRequestCommand command, AuthenticatedActor actor) {
        return assignService.execute(command, actor);
    }

    RequestSummary attend(AttendRequestCommand command, AuthenticatedActor actor) {
        return attendService.execute(command, actor);
    }

    RequestSummary close(CloseRequestCommand command, AuthenticatedActor actor) {
        return closeService.execute(command, actor);
    }

    RequestSummary cancel(CancelRequestCommand command, AuthenticatedActor actor) {
        return cancelService.execute(command, actor);
    }

    RequestSummary reject(RejectRequestCommand command, AuthenticatedActor actor) {
        return rejectService.execute(command, actor);
    }

    @Override
    public RequestHistoryDetail addInternalNote(AddInternalNoteCommand command) {
        return addInternalNoteService.addInternalNote(command);
    }
}
