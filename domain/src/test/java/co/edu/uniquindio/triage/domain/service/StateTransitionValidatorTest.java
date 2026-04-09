package co.edu.uniquindio.triage.domain.service;

import co.edu.uniquindio.triage.domain.enums.RequestStatus;
import co.edu.uniquindio.triage.domain.exception.InvalidStateTransitionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class StateTransitionValidatorTest {

    @Test
    @DisplayName("Should allow valid transitions from REGISTERED")
    void shouldAllowValidTransitionsFromRegistered() {
        assertTrue(StateTransitionValidator.canTransition(RequestStatus.REGISTERED, RequestStatus.CLASSIFIED));
        assertTrue(StateTransitionValidator.canTransition(RequestStatus.REGISTERED, RequestStatus.CANCELLED));
        assertTrue(StateTransitionValidator.canTransition(RequestStatus.REGISTERED, RequestStatus.REJECTED));
    }

    @Test
    @DisplayName("Should forbid invalid transitions from REGISTERED")
    void shouldForbidInvalidTransitionsFromRegistered() {
        assertFalse(StateTransitionValidator.canTransition(RequestStatus.REGISTERED, RequestStatus.IN_PROGRESS));
        assertFalse(StateTransitionValidator.canTransition(RequestStatus.REGISTERED, RequestStatus.ATTENDED));
        assertFalse(StateTransitionValidator.canTransition(RequestStatus.REGISTERED, RequestStatus.CLOSED));
    }

    @Test
    @DisplayName("Should return false when either status is null")
    void shouldReturnFalseWhenEitherStatusIsNull() {
        assertFalse(StateTransitionValidator.canTransition(null, RequestStatus.REGISTERED));
        assertFalse(StateTransitionValidator.canTransition(RequestStatus.REGISTERED, null));
        assertFalse(StateTransitionValidator.canTransition(null, null));
    }

    @Test
    @DisplayName("Should throw exception for invalid transition")
    void shouldThrowExceptionForInvalidTransition() {
        assertThrows(InvalidStateTransitionException.class, 
            () -> StateTransitionValidator.validateTransition(RequestStatus.CLOSED, RequestStatus.REGISTERED));
    }

    @Test
    @DisplayName("Should not throw exception for valid transition")
    void shouldNotThrowExceptionForValidTransition() {
        assertDoesNotThrow(() -> StateTransitionValidator.validateTransition(RequestStatus.REGISTERED, RequestStatus.CLASSIFIED));
    }

    @Test
    @DisplayName("Should return empty set for null status in getNextStates")
    void shouldReturnEmptySetForNullStatusInGetNextStates() {
        assertTrue(StateTransitionValidator.getNextStates(null).isEmpty());
    }

    @Test
    @DisplayName("Should return next states for REGISTERED")
    void shouldReturnNextStatesForRegistered() {
        Set<RequestStatus> nextStates = StateTransitionValidator.getNextStates(RequestStatus.REGISTERED);
        assertEquals(3, nextStates.size());
        assertTrue(nextStates.contains(RequestStatus.CLASSIFIED));
        assertTrue(nextStates.contains(RequestStatus.CANCELLED));
        assertTrue(nextStates.contains(RequestStatus.REJECTED));
    }

    @ParameterizedTest
    @EnumSource(value = RequestStatus.class, names = {"CLOSED", "CANCELLED", "REJECTED"})
    @DisplayName("Should return true for terminal states")
    void shouldReturnTrueForTerminalStates(RequestStatus status) {
        assertTrue(StateTransitionValidator.isTerminal(status));
    }

    @ParameterizedTest
    @EnumSource(value = RequestStatus.class, names = {"REGISTERED", "CLASSIFIED", "IN_PROGRESS", "ATTENDED"})
    @DisplayName("Should return false for non-terminal states")
    void shouldReturnFalseForNonTerminalStates(RequestStatus status) {
        assertFalse(StateTransitionValidator.isTerminal(status));
    }

    @Test
    @DisplayName("Should return all terminal states")
    void shouldReturnAllTerminalStates() {
        Set<RequestStatus> terminalStates = StateTransitionValidator.getTerminalStates();
        assertEquals(3, terminalStates.size());
        assertTrue(terminalStates.contains(RequestStatus.CLOSED));
        assertTrue(terminalStates.contains(RequestStatus.CANCELLED));
        assertTrue(terminalStates.contains(RequestStatus.REJECTED));
    }
}
