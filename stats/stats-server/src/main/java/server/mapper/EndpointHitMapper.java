package server.mapper;

import model.EndpointHitDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import server.entity.EndpointHitEntity;

@Mapper(componentModel = "spring")
public interface EndpointHitMapper {

    @Mapping(target = "id", ignore = true)
    EndpointHitEntity toEntity(EndpointHitDto endpointHitDto);

    EndpointHitDto toDto(EndpointHitEntity entity);
}