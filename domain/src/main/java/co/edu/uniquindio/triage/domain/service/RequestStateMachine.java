package co.edu.uniquindio.triage.domain.service;

import co.edu.uniquindio.triage.domain.enums.RequestStatusEnum;
import co.edu.uniquindio.triage.domain.exception.InvalidStateTransitionException;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public class RequestStateMachine {

    private static final Map<RequestStatusEnum, Set<RequestStatusEnum>> TRANSITIONS;

    static {
        TRANSITIONS = new EnumMap<>(RequestStatusEnum.class);

        TRANSITIONS.put(RequestStatusEnum.REGISTERED,
                EnumSet.of(
                        RequestStatusEnum.CLASSIFIED,
                        RequestStatusEnum.CANCELLED,
                        RequestStatusEnum.REJECTED
                )
        );

        TRANSITIONS.put(RequestStatusEnum.CLASSIFIED,
                EnumSet.of(
                        RequestStatusEnum.IN_PROGRESS,
                        RequestStatusEnum.CANCELLED
                )
        );

        TRANSITIONS.put(RequestStatusEnum.IN_PROGRESS,
                EnumSet.of(
                        RequestStatusEnum.ATTENDED
                )
        );

        TRANSITIONS.put(RequestStatusEnum.ATTENDED,
                EnumSet.of(
                        RequestStatusEnum.CLOSED
                )
        );

        TRANSITIONS.put(RequestStatusEnum.CLOSED, EnumSet.noneOf(RequestStatusEnum.class));
        TRANSITIONS.put(RequestStatusEnum.CANCELLED, EnumSet.noneOf(RequestStatusEnum.class));
        TRANSITIONS.put(RequestStatusEnum.REJECTED, EnumSet.noneOf(RequestStatusEnum.class));
    }

    public static boolean puedeTransicionar(RequestStatusEnum desde, RequestStatusEnum hacia) {
        if (desde == null || hacia == null) {
            return false;
        }
        Set<RequestStatusEnum> estadosPermitidos = TRANSITIONS.get(desde);
        return estadosPermitidos != null && estadosPermitidos.contains(hacia);
    }

    public static void validarTransicion(RequestStatusEnum desde, RequestStatusEnum hacia) {
        if (!puedeTransicionar(desde, hacia)) {
            throw new InvalidStateTransitionException(desde, hacia);
        }
    }

    public static Set<RequestStatusEnum> getEstadosSiguientes(RequestStatusEnum estadoActual) {
        if (estadoActual == null) {
            return EnumSet.noneOf(RequestStatusEnum.class);
        }
        Set<RequestStatusEnum> siguientes = TRANSITIONS.get(estadoActual);
        return siguientes != null ? EnumSet.copyOf(siguientes) : EnumSet.noneOf(RequestStatusEnum.class);
    }

    public static boolean esEstadoTerminal(RequestStatusEnum estado) {
        return estado == RequestStatusEnum.CLOSED
                || estado == RequestStatusEnum.CANCELLED
                || estado == RequestStatusEnum.REJECTED;
    }

    public static Set<RequestStatusEnum> getEstadosTerminales() {
        return EnumSet.of(
                RequestStatusEnum.CLOSED,
                RequestStatusEnum.CANCELLED,
                RequestStatusEnum.REJECTED
        );
    }
}
