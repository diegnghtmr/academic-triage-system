package co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.mapper;

import co.edu.uniquindio.triage.domain.model.OriginChannel;
import co.edu.uniquindio.triage.domain.model.RequestType;
import co.edu.uniquindio.triage.domain.model.id.OriginChannelId;
import co.edu.uniquindio.triage.domain.model.id.RequestTypeId;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.entity.OriginChannelJpaEntity;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.entity.RequestTypeJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CatalogPersistenceMapper {

    @Mapping(target = "id", expression = "java(new RequestTypeId(entity.getId()))")
    RequestType toDomain(RequestTypeJpaEntity entity);

    @Mapping(target = "id", expression = "java(requestType.getId() != null ? requestType.getId().value() : null)")
    RequestTypeJpaEntity toEntity(RequestType requestType);

    @Mapping(target = "id", expression = "java(new OriginChannelId(entity.getId()))")
    OriginChannel toDomain(OriginChannelJpaEntity entity);

    @Mapping(target = "id", expression = "java(originChannel.getId() != null ? originChannel.getId().value() : null)")
    OriginChannelJpaEntity toEntity(OriginChannel originChannel);
}
