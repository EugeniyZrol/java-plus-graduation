package ru.practicum.participationRequest.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.interaction.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.interaction.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.interaction.dto.request.ParticipationRequestDto;
import ru.practicum.participationRequest.service.ParticipationRequestService;

import java.util.List;

@RestController
@RequestMapping("/users/{userId}/events/{eventId}/requests")
@RequiredArgsConstructor
public class EventRequestController {
    private final ParticipationRequestService requestService;

    @GetMapping
    public List<ParticipationRequestDto> getRequestsByEvent(@PathVariable Long userId,
                                                            @PathVariable Long eventId) {
        return requestService.getRequestsByEvent(userId, eventId);
    }

    @PatchMapping
    public EventRequestStatusUpdateResult changeRequestStatus(@PathVariable Long userId,
                                                              @PathVariable Long eventId,
                                                              @Valid @RequestBody EventRequestStatusUpdateRequest request) {
        return requestService.changeRequestStatus(userId, eventId, request);
    }
}