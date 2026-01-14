package ewm.participationRequest.mapper;

import ewm.participationRequest.dto.ParticipationRequestDto;
import ewm.participationRequest.model.ParticipationRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ParticipationRequestMapper {

    @Mapping(source = "event.id", target = "event")
    @Mapping(source = "requester.id", target = "requester")
    @Mapping(source = "status", target = "status")
    ParticipationRequestDto toDto(ParticipationRequest request);
}
