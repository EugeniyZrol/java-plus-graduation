package ru.practicum.event.service;

import ru.practicum.interaction.dto.event.*;
import ru.practicum.event.model.Event;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Set;

public interface EventService {
    List<EventShortDto> getEvents(Long userId, Pageable pageable);

    EventFullDto createEvent(Long userId, NewEventDto newEventDto);

    EventFullDto getEvent(Long userId, Long eventId, String ip);

    EventFullDto updateEvent(Long userId, Long eventId, UpdateEventUserRequest request);

    List<EventShortDto> getPublicEvents(PublicEventSearchRequest requestParams, Pageable pageable, String ip);

    EventFullDto getPublicEventById(Long eventId, String ip);

    boolean existsById(Long eventId);

    Long getEventInitiatorId(Long eventId);

    Set<Event> getEventsByIds(Set<Long> eventIds);

    boolean existEventsByIds(Set<Long> eventIds);

    Integer getParticipantLimit(Long eventId);

    Boolean isRequestModerationEnabled(Long eventId);

    String getEventState(Long eventId);
}