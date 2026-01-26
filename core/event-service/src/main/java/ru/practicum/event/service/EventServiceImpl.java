package ru.practicum.event.service;

import ru.practicum.categories.service.CategoryService;
import ru.practicum.event.repository.specification.EventSpecifications;
import ru.practicum.interaction.client.feign.UserClient;
import ru.practicum.interaction.dto.categories.CategoryDto;
import ru.practicum.interaction.dto.event.*;
import ru.practicum.interaction.dto.user.UserShortDto;
import ru.practicum.event.mapper.EventMapper;
import ru.practicum.event.model.Event;
import ru.practicum.interaction.enums.event.EventState;
import ru.practicum.interaction.enums.event.StateAction;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.interaction.validation.EventValidationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.interaction.exception.NotFoundException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {
    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final EventStatsService eventStatsService;
    private final UserClient userClient;
    private final CategoryService categoryService;

    @Override
    public List<EventShortDto> getEvents(Long userId, Pageable pageable) {
        if (Boolean.FALSE.equals(userClient.existsUserById(userId))) {
            throw new NotFoundException("User not found with id: " + userId);
        }

        Page<Event> eventsPage = eventRepository.findAllByInitiatorIdOrderByCreatedAtDesc(userId, pageable);
        List<Event> events = eventsPage.getContent();

        Map<Long, CategoryDto> categories = categoryService.getCategoriesByIds(
                events.stream().map(Event::getCategoryId).collect(Collectors.toSet())
        );
        Map<Long, UserShortDto> users = userClient.getUsersShortByIds(
                events.stream().map(Event::getInitiatorId).collect(Collectors.toSet())
        );

        return eventStatsService.enrichEventsShortDtoBatch(events, categories, users);
    }

    @Override
    public EventFullDto getEvent(Long userId, Long eventId, String ip) {
        if (Boolean.FALSE.equals(userClient.existsUserById(userId))) {
            throw new NotFoundException("User not found with id: " + userId);
        }

        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event not found"));

        eventStatsService.recordHit("/events/" + eventId, ip);

        CategoryDto category = categoryService.getCategoryById(event.getCategoryId());
        UserShortDto initiator = userClient.getUserShortById(event.getInitiatorId());

        return eventStatsService.enrichEventFullDto(event, category, initiator);
    }

    @Override
    @Transactional
    public EventFullDto createEvent(Long userId, NewEventDto newEventDto) {
        EventValidationUtils.validateEventDate(newEventDto.getEventDate(), 2);

        if (Boolean.FALSE.equals(userClient.existsUserById(userId))) {
            throw new NotFoundException("User not found with id: " + userId);
        }

        if (newEventDto.getCategory() != null) {
            categoryService.getCategoryById(newEventDto.getCategory());
        }

        Event event = eventMapper.toEvent(newEventDto);
        event.setInitiatorId(userId);
        event.setCreatedAt(LocalDateTime.now());
        event.setState(EventState.PENDING);

        Event savedEvent = eventRepository.save(event);

        CategoryDto category = categoryService.getCategoryById(savedEvent.getCategoryId());
        UserShortDto initiator = userClient.getUserShortById(savedEvent.getInitiatorId());
        EventFullDto eventDto = eventMapper.toFullDto(savedEvent, category, initiator);
        eventDto.setConfirmedRequests(0L);
        eventDto.setViews(0L);
        return eventDto;
    }

    @Override
    @Transactional
    public EventFullDto updateEvent(Long userId, Long eventId, UpdateEventUserRequest request) {
        if (Boolean.FALSE.equals(userClient.existsUserById(userId))) {
            throw new NotFoundException("User not found with id: " + userId);
        }

        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event not found"));

        EventValidationInfo validationInfo = EventValidationInfo.builder()
                .state(event.getState().toString())
                .eventDate(event.getEventDate())
                .build();
        EventValidationUtils.validateEventStateForUpdate(validationInfo);

        if (request.getEventDate() != null) {
            EventValidationUtils.validateEventDate(request.getEventDate(), 2);
        }

        if (request.getParticipantLimit() != null) {
            EventValidationUtils.validateParticipantLimit(request.getParticipantLimit());
        }

        if (request.getCategory() != null) {
            categoryService.getCategoryById(request.getCategory());
        }

        eventMapper.updateEventFromUserRequest(request, event);

        if (request.getStateAction() != null) {
            StateAction stateAction = StateAction.valueOf(request.getStateAction());
            if (stateAction == StateAction.SEND_TO_REVIEW) {
                event.setState(EventState.PENDING);
            } else if (stateAction == StateAction.CANCEL_REVIEW) {
                event.setState(EventState.CANCELED);
            }
        }

        Event updatedEvent = eventRepository.save(event);

        CategoryDto category = categoryService.getCategoryById(updatedEvent.getCategoryId());
        UserShortDto initiator = userClient.getUserShortById(updatedEvent.getInitiatorId());

        return eventStatsService.enrichEventFullDto(updatedEvent, category, initiator);
    }

    @Override
    public List<EventShortDto> getPublicEvents(PublicEventSearchRequest requestParams, Pageable pageable, String ip) {
        EventValidationUtils.validateDateRange(requestParams.getRangeStart(), requestParams.getRangeEnd());

        Specification<Event> spec = buildPublicEventsSpecification(requestParams);
        List<Event> events = eventRepository.findAll(spec, pageable).getContent();

        Map<Long, CategoryDto> categories = categoryService.getCategoriesByIds(
                events.stream().map(Event::getCategoryId).collect(Collectors.toSet())
        );
        Map<Long, UserShortDto> users = userClient.getUsersShortByIds(
                events.stream().map(Event::getInitiatorId).collect(Collectors.toSet())
        );

        eventStatsService.recordHit("/events", ip);
        return eventStatsService.enrichEventsShortDtoBatch(events, categories, users);
    }

    @Override
    public EventFullDto getPublicEventById(Long eventId, String ip) {
        Event event = eventRepository.findById(eventId)
                .filter(e -> EventState.PUBLISHED.equals(e.getState()))
                .orElseThrow(() -> new NotFoundException("Event not found"));

        eventStatsService.recordHit("/events/" + eventId, ip);

        CategoryDto category = categoryService.getCategoryById(event.getCategoryId());
        UserShortDto initiator = userClient.getUserShortById(event.getInitiatorId());

        return eventStatsService.enrichEventFullDto(event, category, initiator);
    }

    @Override
    public boolean existsById(Long eventId) {
        return eventRepository.existsById(eventId);
    }

    @Override
    public Long getEventInitiatorId(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));
        return event.getInitiatorId();
    }

    @Override
    public String getEventState(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));
        return event.getState().toString();
    }

    @Override
    public Integer getParticipantLimit(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));
        return event.getParticipantLimit();
    }

    @Override
    public Boolean isRequestModerationEnabled(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));
        return event.getIsRequestModeration();
    }

    private Specification<Event> buildPublicEventsSpecification(PublicEventSearchRequest params) {
        Specification<Event> spec = Specification.where(EventSpecifications.isPublished());

        if (params.getText() != null && !params.getText().trim().isEmpty()) {
            spec = spec.and(EventSpecifications.containsText(params.getText()));
        }

        if (params.getCategories() != null && !params.getCategories().isEmpty()) {
            spec = spec.and(EventSpecifications.hasCategories(params.getCategories()));
        }

        if (params.getPaid() != null) {
            spec = spec.and(EventSpecifications.isPaid(params.getPaid()));
        }

        LocalDateTime actualRangeStart = (params.getRangeStart() == null && params.getRangeEnd() == null) ?
                LocalDateTime.now() : params.getRangeStart();

        if (actualRangeStart != null) {
            spec = spec.and(EventSpecifications.startsAfter(actualRangeStart));
        }

        if (params.getRangeEnd() != null) {
            spec = spec.and(EventSpecifications.endsBefore(params.getRangeEnd()));
        }

        return spec;
    }

    @Override
    public Set<Event> getEventsByIds(Set<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return new HashSet<>();
        }
        List<Event> events = eventRepository.findAllById(eventIds);
        return new HashSet<>(events);
    }

    @Override
    public boolean existEventsByIds(Set<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return true;
        }
        return eventRepository.countByIdIn(eventIds) == eventIds.size();
    }
}