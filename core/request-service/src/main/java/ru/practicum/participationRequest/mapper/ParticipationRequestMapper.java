package ru.practicum.participationRequest.mapper;

import ru.practicum.interaction.dto.request.ParticipationRequestDto;
import ru.practicum.participationRequest.model.ParticipationRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ParticipationRequestMapper {

    @Mapping(source = "eventId", target = "event")
    @Mapping(source = "requesterId", target = "requester")
    ParticipationRequestDto toDto(ParticipationRequest request);
}