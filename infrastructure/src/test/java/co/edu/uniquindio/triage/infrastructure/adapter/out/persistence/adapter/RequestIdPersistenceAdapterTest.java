package co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.adapter;

import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.repository.RequestJpaRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class RequestIdPersistenceAdapterTest {

    private final RequestJpaRepository requestJpaRepository = mock(RequestJpaRepository.class);
    private final RequestIdPersistenceAdapter adapter = new RequestIdPersistenceAdapter(requestJpaRepository);

    @Test
    void nextIdMustUseRepositoryAutoIncrementValue() {
        given(requestJpaRepository.findNextAutoIncrementValue()).willReturn(Optional.of(42L));

        var nextId = adapter.nextId();

        assertThat(nextId.value()).isEqualTo(42L);
    }

    @Test
    void nextIdMustFailWhenAutoIncrementMetadataIsUnavailable() {
        given(requestJpaRepository.findNextAutoIncrementValue()).willReturn(Optional.empty());

        assertThatThrownBy(adapter::nextId)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No se pudo reservar el próximo id de solicitud");
    }
}
