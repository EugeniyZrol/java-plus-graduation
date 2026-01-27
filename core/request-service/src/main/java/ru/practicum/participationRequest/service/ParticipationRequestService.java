package ru.practicum.participationRequest.service;

import ru.practicum.interaction.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.interaction.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.interaction.dto.request.ParticipationRequestDto;

import java.util.List;

public interface ParticipationRequestService {

    List<ParticipationRequestDto> getUserRequests(Long userId);

    ParticipationRequestDto createRequest(Long userId, Long eventId);

    ParticipationRequestDto cancelRequest(Long userId, Long requestId);

    List<ParticipationRequestDto> getRequestsByEvent(Long userId, Long eventId);

    EventRequestStatusUpdateResult changeRequestStatus(Long userId, Long eventId,
                                                       EventRequestStatusUpdateRequest request);

    int getConfirmedRequestsCountForFeign(Long eventId);
}
