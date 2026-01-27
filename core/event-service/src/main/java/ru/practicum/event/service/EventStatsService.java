package ru.practicum.event.service;

import client.StatsClient;
import ru.practicum.interaction.client.feign.RequestClient;
import ru.practicum.interaction.dto.categories.CategoryDto;
import ru.practicum.interaction.dto.event.EventFullDto;
import ru.practicum.interaction.dto.event.EventShortDto;
import ru.practicum.event.mapper.EventMapper;
import ru.practicum.event.model.Event;
import ru.practicum.event.repository.EventRepository;
import model.ViewStatsDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.interaction.dto.user.UserShortDto;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventStatsService {
    private final StatsClient statsClient;
    private final RequestClient requestClient;
    private final EventRepository eventRepository;
    private final EventMapper eventMapper;

    public EventFullDto enrichEventFullDto(Event event, CategoryDto category, UserShortDto initiator) {
        EventFullDto dto = eventMapper.toFullDto(event, category, initiator);

        Long confirmedRequests = getConfirmedRequests(event.getId());
        dto.setConfirmedRequests(confirmedRequests);

        Long views = getViews(event.getId());
        dto.setViews(views);

        return dto;
    }

    public List<EventShortDto> enrichEventsShortDtoBatch(List<Event> events,
                                                         Map<Long, CategoryDto> categories,
                                                         Map<Long, UserShortDto> users) {
        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .collect(Collectors.toList());

        Map<Long, Long> viewsMap = getViewsForEventsBatch(eventIds);
        Map<Long, Long> requestsMap = getConfirmedRequestsBatch(eventIds);

        return events.stream()
                .map(event -> {
                    EventShortDto dto = eventMapper.toShortDto(
                            event,
                            categories.get(event.getCategoryId()),
                            users.get(event.getInitiatorId())
                    );
                    dto.setViews(viewsMap.getOrDefault(event.getId(), 0L));
                    dto.setConfirmedRequests(requestsMap.getOrDefault(event.getId(), 0L));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    public List<EventFullDto> enrichEventsFullDtoBatch(List<Event> events,
                                                       Map<Long, CategoryDto> categories,
                                                       Map<Long, UserShortDto> users) {
        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .collect(Collectors.toList());

        Map<Long, Long> viewsMap = getViewsForEventsBatch(eventIds);
        Map<Long, Long> requestsMap = getConfirmedRequestsBatch(eventIds);

        return events.stream()
                .map(event -> {
                    EventFullDto dto = eventMapper.toFullDto(
                            event,
                            categories.get(event.getCategoryId()),
                            users.get(event.getInitiatorId())
                    );
                    dto.setViews(viewsMap.getOrDefault(event.getId(), 0L));
                    dto.setConfirmedRequests(requestsMap.getOrDefault(event.getId(), 0L));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    public Map<Long, Long> getViewsForEventsBatch(List<Long> eventIds) {
        if (eventIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<String> uris = eventIds.stream()
                .map(id -> "/events/" + id)
                .collect(Collectors.toList());

        LocalDateTime start = getEarliestEventDate();
        LocalDateTime end = LocalDateTime.now();

        log.debug("Запрос статистики просмотров для {} событий с {} по {}",
                eventIds.size(), start, end);

        List<ViewStatsDto> stats = statsClient.getStats(start, end, uris, true);

        return createViewsMap(eventIds, stats);
    }

    public void recordHit(String path, String ip) {
        try {
            log.debug("Запись hit для пути {} с IP: {}", path, ip);
            statsClient.hit(path, ip);
        } catch (Exception e) {
            log.error("Ошибка при записи статистики для пути {}: {}", path, e.getMessage(), e);
        }
    }


    public Map<Long, Long> getConfirmedRequestsBatch(List<Long> eventIds) {
        if (eventIds.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            Map<Long, Integer> result = requestClient.getConfirmedRequestsCountBatch(eventIds);

            Map<Long, Long> confirmedRequestsMap = new HashMap<>();
            eventIds.forEach(id -> confirmedRequestsMap.put(id, 0L));

            if (result != null && !result.isEmpty()) {
                result.forEach((eventId, count) ->
                        confirmedRequestsMap.put(eventId, count.longValue())
                );
            }

            log.debug("Получены подтвержденные запросы для {} событий", eventIds.size());
            return confirmedRequestsMap;

        } catch (Exception e) {
            log.error("Ошибка при получении подтвержденных запросов: {}", e.getMessage(), e);
            return eventIds.stream()
                    .collect(Collectors.toMap(id -> id, id -> 0L));
        }
    }


    public Long getConfirmedRequests(Long eventId) {
        Map<Long, Long> requestsMap = getConfirmedRequestsBatch(Collections.singletonList(eventId));
        return requestsMap.getOrDefault(eventId, 0L);
    }


    public Long getViews(Long eventId) {
        Map<Long, Long> viewsMap = getViewsForEventsBatch(Collections.singletonList(eventId));
        return viewsMap.getOrDefault(eventId, 0L);
    }


    private Map<Long, Long> createViewsMap(List<Long> eventIds, List<ViewStatsDto> stats) {
        Map<Long, Long> viewsMap = eventIds.stream()
                .collect(Collectors.toMap(id -> id, id -> 0L));

        if (stats != null && !stats.isEmpty()) {
            stats.forEach(stat -> {
                Long eventId = extractEventIdFromUri(stat.getUri());
                if (eventId != -1L) {
                    viewsMap.put(eventId, stat.getHits());
                }
            });
        }

        return viewsMap;
    }

    private Long extractEventIdFromUri(String uri) {
        try {
            String[] parts = uri.split("/");
            String lastPart = parts[parts.length - 1];
            return Long.parseLong(lastPart);
        } catch (Exception e) {
            log.warn("Не удалось извлечь ID события из URI: {}", uri);
            return -1L;
        }
    }

    private LocalDateTime getEarliestEventDate() {
        try {
            Event earliestEvent = eventRepository.findFirstByOrderByCreatedAtAsc();
            return (earliestEvent != null)
                    ? earliestEvent.getCreatedAt()
                    : LocalDateTime.now().minusYears(1);
        } catch (Exception e) {
            log.warn("Не удалось получить дату самого раннего события: {}", e.getMessage());
            return LocalDateTime.now().minusYears(1);
        }
    }
}