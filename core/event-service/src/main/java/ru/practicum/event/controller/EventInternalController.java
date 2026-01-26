package ru.practicum.event.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.event.mapper.EventMapper;
import ru.practicum.event.model.Event;
import ru.practicum.event.service.EventService;
import ru.practicum.interaction.client.feign.UserClient;
import ru.practicum.interaction.dto.categories.CategoryDto;
import ru.practicum.interaction.dto.event.EventFullDto;
import ru.practicum.interaction.dto.event.EventShortDto;
import ru.practicum.interaction.dto.user.UserShortDto;
import ru.practicum.interaction.enums.event.EventState;
import ru.practicum.categories.service.CategoryService;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/events/feign")
@RequiredArgsConstructor
public class EventInternalController {
    private final EventService eventService;
    private final EventMapper eventMapper;
    private final CategoryService categoryService;
    private final UserClient userClient;

    @GetMapping("/{eventId}/exists")
    public Boolean existsEventById(@PathVariable Long eventId) {
        return eventService.existsById(eventId);
    }

    @GetMapping("/{eventId}/initiator")
    public Long getEventInitiatorId(@PathVariable Long eventId) {
        return eventService.getEventInitiatorId(eventId);
    }

    @GetMapping("/{eventId}/state")
    public String getEventState(@PathVariable Long eventId) {
        return eventService.getEventState(eventId);
    }

    @GetMapping("/{eventId}/participant-limit")
    public Integer getParticipantLimit(@PathVariable Long eventId) {
        return eventService.getParticipantLimit(eventId);
    }

    @GetMapping("/{eventId}/moderation")
    public Boolean isRequestModerationEnabled(@PathVariable Long eventId) {
        return eventService.isRequestModerationEnabled(eventId);
    }

    @GetMapping("/{eventId}")
    public EventFullDto getEventById(@PathVariable Long eventId) {
        Event event = eventService.getEventByIdForFeign(eventId);

        CategoryDto category = categoryService.getCategoryById(event.getCategoryId());
        UserShortDto initiator = userClient.getUserShortById(event.getInitiatorId());

        EventFullDto dto = eventMapper.toFullDto(event, category, initiator);
        dto.setConfirmedRequests(0L);
        dto.setViews(0L);
        return dto;
    }

    @PostMapping("/batch")
    public List<EventShortDto> getEventsByIds(@RequestBody Set<Long> eventIds) {
        Set<Event> events = eventService.getEventsByIds(eventIds);
        List<Event> publishedEvents = events.stream()
                .filter(e -> EventState.PUBLISHED.equals(e.getState()))
                .toList();

        return publishedEvents.stream()
                .map(eventMapper::toShortDtoWithoutRelations)
                .collect(Collectors.toList());
    }
}