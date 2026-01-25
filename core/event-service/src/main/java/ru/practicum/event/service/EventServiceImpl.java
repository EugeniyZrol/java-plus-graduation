package ru.practicum.event.service;

import ru.practicum.categories.service.CategoryService;
import ru.practicum.event.repository.specification.EventSpecifications;
import ru.practicum.interaction.client.feign.UserClient;
import ru.practicum.interaction.dto.event.*;
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

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        Boolean userExists = userClient.existsUserById(userId);
        if (Boolean.FALSE.equals(userExists)) {
            throw new ru.practicum.interaction.exception.NotFoundException("User not found with id: " + userId);
        }

        Page<Event> eventsPage = eventRepository.findAllByInitiatorIdOrderByCreatedAtDesc(userId, pageable);
        return eventStatsService.enrichEventsShortDtoBatch(eventsPage.getContent(), eventMapper);
    }

    @Override
    public EventFullDto getEvent(Long userId, Long eventId, String ip) {
        Boolean userExists = userClient.existsUserById(userId);
        if (Boolean.FALSE.equals(userExists)) {
            throw new ru.practicum.interaction.exception.NotFoundException("User not found with id: " + userId);
        }

        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new ru.practicum.interaction.exception.NotFoundException("Event not found"));

        eventStatsService.recordHit("/events/" + eventId, ip);
        return eventStatsService.enrichEventFullDto(event, eventMapper);
    }

    @Override
    @Transactional
    public EventFullDto createEvent(Long userId, NewEventDto newEventDto) {
        EventValidationUtils.validateEventDate(newEventDto.getEventDate(), 2);

        Boolean userExists = userClient.existsUserById(userId);
        if (Boolean.FALSE.equals(userExists)) {
            throw new ru.practicum.interaction.exception.NotFoundException("User not found with id: " + userId);
        }

        if (newEventDto.getCategory() != null) {
            try {
                categoryService.getCategoryById(newEventDto.getCategory());
            } catch (ru.practicum.interaction.exception.NotFoundException e) {
                throw new ru.practicum.interaction.exception.NotFoundException("Category not found");
            }
        }

        Event event = eventMapper.toEvent(newEventDto);
        event.setInitiatorId(userId);
        event.setCreatedAt(LocalDateTime.now());
        event.setState(EventState.PENDING);

        Event savedEvent = eventRepository.save(event);

        EventFullDto eventDto = eventMapper.toFullDto(savedEvent);
        eventDto.setConfirmedRequests(0L);
        eventDto.setViews(0L);
        return eventDto;
    }

    @Override
    @Transactional
    public EventFullDto updateEvent(Long userId, Long eventId, UpdateEventUserRequest request) {
        Boolean userExists = userClient.existsUserById(userId);
        if (Boolean.FALSE.equals(userExists)) {
            throw new ru.practicum.interaction.exception.NotFoundException("User not found with id: " + userId);
        }

        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new ru.practicum.interaction.exception.NotFoundException("Event not found"));

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
            try {
                categoryService.getCategoryById(request.getCategory());
            } catch (ru.practicum.interaction.exception.NotFoundException e) {
                throw new ru.practicum.interaction.exception.NotFoundException("Category not found");
            }
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
        return eventStatsService.enrichEventFullDto(updatedEvent, eventMapper);
    }

    @Override
    public List<EventShortDto> getPublicEvents(PublicEventSearchRequest requestParams, Pageable pageable, String ip) {
        EventValidationUtils.validateDateRange(requestParams.getRangeStart(), requestParams.getRangeEnd());

        Specification<Event> spec = buildPublicEventsSpecification(requestParams);

        List<Event> events = eventRepository.findAll(spec, pageable).getContent();
        List<EventShortDto> result = eventStatsService.enrichEventsShortDtoBatch(events, eventMapper);

        eventStatsService.recordHit("/events", ip);
        return result;
    }

    @Override
    public EventFullDto getPublicEventById(Long eventId, String ip) {
        Event event = eventRepository.findById(eventId)
                .filter(e -> EventState.PUBLISHED.equals(e.getState()))
                .orElseThrow(() -> new ru.practicum.interaction.exception.NotFoundException("Event not found"));

        eventStatsService.recordHit("/events/" + eventId, ip);
        return eventStatsService.enrichEventFullDto(event, eventMapper);
    }

    @Override
    public boolean existsById(Long eventId) {
        return eventRepository.existsById(eventId);
    }

    @Override
    public Long getEventInitiatorId(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ru.practicum.interaction.exception.NotFoundException("Event not found"));
        return event.getInitiatorId();
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

    @Override
    public Integer getParticipantLimit(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ru.practicum.interaction.exception.NotFoundException("Event not found"));
        return event.getParticipantLimit();
    }

    @Override
    public Boolean isRequestModerationEnabled(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ru.practicum.interaction.exception.NotFoundException("Event not found"));
        return event.getIsRequestModeration();
    }

    @Override
    public String getEventState(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ru.practicum.interaction.exception.NotFoundException("Event not found"));
        return event.getState().toString();
    }
}