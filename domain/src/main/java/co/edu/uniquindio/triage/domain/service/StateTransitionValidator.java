package co.edu.uniquindio.triage.domain.service;

import co.edu.uniquindio.triage.domain.enums.RequestStatus;
import co.edu.uniquindio.triage.domain.exception.InvalidStateTransitionException;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public class StateTransitionValidator {

    private static final Map<RequestStatus, Set<RequestStatus>> TRANSITIONS;

    static {
        TRANSITIONS = new EnumMap<>(RequestStatus.class);

        TRANSITIONS.put(RequestStatus.REGISTERED,
                EnumSet.of(
                        RequestStatus.CLASSIFIED,
                        RequestStatus.CANCELLED,
                        RequestStatus.REJECTED
                )
        );

        TRANSITIONS.put(RequestStatus.CLASSIFIED,
                EnumSet.of(
                        RequestStatus.IN_PROGRESS,
                        RequestStatus.CANCELLED
                )
        );

        TRANSITIONS.put(RequestStatus.IN_PROGRESS,
                EnumSet.of(
                        RequestStatus.ATTENDED
                )
        );

        TRANSITIONS.put(RequestStatus.ATTENDED,
                EnumSet.of(
                        RequestStatus.CLOSED
                )
        );

        TRANSITIONS.put(RequestStatus.CLOSED, EnumSet.noneOf(RequestStatus.class));
        TRANSITIONS.put(RequestStatus.CANCELLED, EnumSet.noneOf(RequestStatus.class));
        TRANSITIONS.put(RequestStatus.REJECTED, EnumSet.noneOf(RequestStatus.class));
    }

    public static boolean canTransition(RequestStatus from, RequestStatus to) {
        if (from == null || to == null) {
            return false;
        }
        Set<RequestStatus> allowed = TRANSITIONS.get(from);
        return allowed != null && allowed.contains(to);
    }

    public static void validateTransition(RequestStatus from, RequestStatus to) {
        if (!canTransition(from, to)) {
            throw new InvalidStateTransitionException(from, to);
        }
    }

    public static Set<RequestStatus> getNextStates(RequestStatus current) {
        if (current == null) {
            return EnumSet.noneOf(RequestStatus.class);
        }
        Set<RequestStatus> next = TRANSITIONS.get(current);
        return next != null ? EnumSet.copyOf(next) : EnumSet.noneOf(RequestStatus.class);
    }

    public static boolean isTerminal(RequestStatus status) {
        return status == RequestStatus.CLOSED
                || status == RequestStatus.CANCELLED
                || status == RequestStatus.REJECTED;
    }

    public static Set<RequestStatus> getTerminalStates() {
        return EnumSet.of(
                RequestStatus.CLOSED,
                RequestStatus.CANCELLED,
                RequestStatus.REJECTED
        );
    }
}
