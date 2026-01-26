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
    private final EventMapper eventMapper; // Маппер внедрен в сервис

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
        if (events.isEmpty()) return List.of();

        List<Long> eventIds = events.stream().map(Event::getId).collect(Collectors.toList());
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
        if (events.isEmpty()) return List.of();

        List<Long> eventIds = events.stream().map(Event::getId).collect(Collectors.toList());
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
        if (eventIds.isEmpty()) return Map.of();

        List<String> uris = eventIds.stream()
                .map(id -> "/events/" + id)
                .collect(Collectors.toList());

        Event earliestEvent = eventRepository.findFirstByOrderByCreatedAtAsc();
        LocalDateTime start = (earliestEvent != null)
                ? earliestEvent.getCreatedAt()
                : LocalDateTime.now().minusYears(1);
        LocalDateTime end = LocalDateTime.now();

        List<ViewStatsDto> stats = statsClient.getStats(start, end, uris, true);
        Map<Long, Long> viewsMap = eventIds.stream()
                .collect(Collectors.toMap(id -> id, id -> 0L));

        if (stats != null) {
            stats.forEach(stat -> {
                Long eventId = extractEventIdFromUri(stat.getUri());
                if (eventId != -1L) viewsMap.put(eventId, stat.getHits());
            });
        }
        return viewsMap;
    }

    public void recordHit(String path, String ip) {
        statsClient.hit(path, ip);
    }

    public Map<Long, Long> getConfirmedRequestsBatch(List<Long> eventIds) {
        if (eventIds.isEmpty()) return Map.of();

        Map<Long, Integer> result = requestClient.getConfirmedRequestsCountBatch(eventIds);
        Map<Long, Long> confirmedRequestsMap = new HashMap<>();
        eventIds.forEach(id -> confirmedRequestsMap.put(id, 0L));

        if (result != null && !result.isEmpty()) {
            result.forEach((key, value) -> confirmedRequestsMap.put(key, value.longValue()));
        }
        log.debug("Получены подтвержденные запросы для {} событий", eventIds.size());
        return confirmedRequestsMap;
    }

    public Long getConfirmedRequests(Long eventId) {
        Map<Long, Long> requestsMap = getConfirmedRequestsBatch(List.of(eventId));
        return requestsMap.getOrDefault(eventId, 0L);
    }

    public Long getViews(Long eventId) {
        Map<Long, Long> viewsMap = getViewsForEventsBatch(List.of(eventId));
        return viewsMap.getOrDefault(eventId, 0L);
    }

    private Long extractEventIdFromUri(String uri) {
        try {
            String[] parts = uri.split("/");
            return Long.parseLong(parts[parts.length - 1]);
        } catch (Exception e) {
            return -1L;
        }
    }
}