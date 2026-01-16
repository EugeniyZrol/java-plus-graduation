package ewm.event.service;

import ewm.event.dto.AdminEventSearchRequest;
import ewm.event.dto.EventFullDto;
import ewm.event.dto.UpdateEventAdminRequest;
import ewm.event.mapper.EventMapper;
import ewm.event.model.Event;
import ewm.event.model.EventState;
import ewm.event.model.StateAction;
import ewm.event.repository.EventRepository;
import ewm.event.repository.specification.EventSpecifications;
import ewm.util.validation.EventValidationUtils;
import ewm.exception.ConflictException;
import ewm.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminEventServiceImpl implements AdminEventService {

    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final EventStatsService eventStatsService;

    @Override
    @Transactional(readOnly = true)
    public List<EventFullDto> getEvents(AdminEventSearchRequest requestParams,
                                        Pageable pageable) {

        EventValidationUtils.validateDateRange(requestParams.getRangeStart(), requestParams.getRangeEnd());

        Specification<Event> specification = buildAdminEventsSpecification(requestParams);

        Page<Long> eventIdsPage = eventRepository.findAll(specification, pageable)
                .map(Event::getId);
        List<Long> eventIds = eventIdsPage.getContent();

        if (eventIds.isEmpty()) {
            return List.of();
        }

        List<Event> events = eventRepository.findAllByIdWithCategoryAndInitiator(eventIds);

        log.debug("Админский поиск событий: найдено {} событий", events.size());

        return eventStatsService.enrichEventsFullDtoBatch(events, eventMapper);
    }

    private Specification<Event> buildAdminEventsSpecification(AdminEventSearchRequest params) {
        Specification<Event> spec = Specification.where(null);

        if (params.getUsers() != null && !params.getUsers().isEmpty()) {
            spec = spec.and(EventSpecifications.hasUsers(params.getUsers()));
        }

        if (params.getStates() != null && !params.getStates().isEmpty()) {
            spec = spec.and(EventSpecifications.hasStates(params.getStates()));
        }

        if (params.getCategories() != null && !params.getCategories().isEmpty()) {
            spec = spec.and(EventSpecifications.hasCategories(params.getCategories()));
        }

        if (params.getRangeStart() != null) {
            spec = spec.and(EventSpecifications.startsAfter(params.getRangeStart()));
        }

        if (params.getRangeEnd() != null) {
            spec = spec.and(EventSpecifications.endsBefore(params.getRangeEnd()));
        }

        if (params.getRangeStart() == null && params.getRangeEnd() == null) {
            spec = spec.and(EventSpecifications.startsAfter(LocalDateTime.now()));
        }

        return spec;
    }

    @Override
    @Transactional
    public EventFullDto updateEvent(Long eventId, UpdateEventAdminRequest request) {
        Event event = eventRepository.findByIdWithCategoryAndInitiator(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с ID=" + eventId + " не найдено"));

        log.debug("Обновление события администратором: ID={}, stateAction={}", eventId, request.getStateAction());

        validateAndUpdateEventState(event, request);
        eventMapper.updateEventFromAdminRequest(request, event);

        Event updatedEvent = eventRepository.save(event);
        log.info("Событие обновлено администратором: ID={}, новое состояние={}", eventId, updatedEvent.getState());

        return eventStatsService.enrichEventFullDto(updatedEvent, eventMapper);
    }

    private void validateAndUpdateEventState(Event event, UpdateEventAdminRequest request) {
        if (request.getStateAction() != null) {
            StateAction stateAction = StateAction.valueOf(request.getStateAction());
            EventState currentState = EventState.valueOf(event.getState());

            if (stateAction == StateAction.PUBLISH_EVENT) {
                validatePublishEvent(event, currentState);
                event.setState(EventState.PUBLISHED.toString());
                event.setPublishedAt(LocalDateTime.now());
                log.debug("Событие опубликовано: ID={}", event.getId());
            } else if (stateAction == StateAction.REJECT_EVENT) {
                validateRejectEvent(currentState);
                event.setState(EventState.CANCELED.toString());
                log.debug("Событие отклонено: ID={}", event.getId());
            }
        }
    }

    private void validatePublishEvent(Event event, EventState currentState) {
        if (currentState != EventState.PENDING) {
            log.warn("Попытка публикации события не в состоянии ожидания: ID={}, текущее состояние={}",
                    event.getId(), currentState);
            throw new ConflictException("Событие можно публиковать, только если оно в состоянии ожидания публикации");
        }
        EventValidationUtils.validateEventDate(event.getEventDate(), 1);
    }

    private void validateRejectEvent(EventState currentState) {
        if (currentState == EventState.PUBLISHED) {
            log.warn("Попытка отклонения уже опубликованного события");
            throw new ConflictException("Событие можно отклонить, только если оно еще не опубликовано");
        }
    }
}