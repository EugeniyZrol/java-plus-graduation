package ewm.event.service;

import client.StatsClient;
import ewm.event.dto.EventFullDto;
import ewm.event.dto.EventShortDto;
import ewm.event.mapper.EventMapper;
import ewm.event.model.Event;
import ewm.event.repository.EventRepository;
import ewm.participationRequest.dto.EventConfirmedRequestsDto;
import ewm.participationRequest.repository.ParticipationRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import model.EndpointHitDto;
import model.ViewStatsDto;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventStatsService {
    private static final String ENDPOINT = "/events";
    private static final String APP_NAME = "ewm-main-service";

    private final StatsClient statsClient;
    private final ParticipationRequestRepository requestRepository;
    private final EventRepository eventRepository;

    public Map<Long, Long> getViewsForEventsBatch(List<Long> eventIds) {
        if (eventIds.isEmpty()) {
            return Map.of();
        }

        List<String> uris = eventIds.stream()
                .map(id -> ENDPOINT + "/" + id)
                .collect(Collectors.toList());

        LocalDateTime start = eventRepository.findFirstByOrderByCreatedAtAsc().getCreatedAt();
        LocalDateTime end = LocalDateTime.now();

        List<ViewStatsDto> stats = statsClient.getStats(start, end, uris, true);

        Map<Long, Long> viewsMap = eventIds.stream()
                .collect(Collectors.toMap(id -> id, id -> 0L));

        if (stats != null) {
            stats.forEach(stat -> {
                Long eventId = extractEventIdFromUri(stat.getUri());
                if (eventId != -1L) {
                    viewsMap.put(eventId, stat.getHits());
                }
            });
        }

        return viewsMap;
    }

    public void recordHit(String path, String ip) {
        EndpointHitDto hitDto = new EndpointHitDto(
                APP_NAME,
                path,
                ip,
                LocalDateTime.now()
        );
        statsClient.hit(hitDto);
    }

    public Map<Long, Long> getConfirmedRequestsBatch(List<Long> eventIds) {
        if (eventIds.isEmpty()) {
            return Map.of();
        }

        List<EventConfirmedRequestsDto> results = requestRepository.findConfirmedRequestsCountByEventIds(eventIds);

        Map<Long, Long> confirmedRequestsMap = eventIds.stream()
                .collect(Collectors.toMap(id -> id, id -> 0L));

        results.forEach(dto -> confirmedRequestsMap.put(dto.getEventId(), dto.getConfirmedCount()));

        log.debug("Получены подтвержденные запросы для {} событий", results.size());
        return confirmedRequestsMap;
    }

    public EventFullDto enrichEventFullDto(Event event, EventMapper eventMapper) {
        EventFullDto dto = eventMapper.toFullDto(event);
        Map<Long, Long> confirmedRequests = getConfirmedRequestsBatch(List.of(event.getId()));
        Map<Long, Long> views = getViewsForEventsBatch(List.of(event.getId()));

        dto.setConfirmedRequests(confirmedRequests.getOrDefault(event.getId(), 0L));
        dto.setViews(views.getOrDefault(event.getId(), 0L));
        return dto;
    }

    private Long extractEventIdFromUri(String uri) {
        try {
            return Long.parseLong(uri.substring(ENDPOINT.length() + 1));
        } catch (Exception e) {
            return -1L;
        }
    }

    public List<EventFullDto> enrichEventsFullDtoBatch(List<Event> events, EventMapper eventMapper) {
        if (events.isEmpty()) {
            return List.of();
        }

        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .collect(Collectors.toList());

        Map<Long, Long> confirmedRequestsMap = getConfirmedRequestsBatch(eventIds);
        Map<Long, Long> viewsMap = getViewsForEventsBatch(eventIds);

        return events.stream()
                .map(event -> {
                    EventFullDto dto = eventMapper.toFullDto(event);
                    dto.setConfirmedRequests(confirmedRequestsMap.getOrDefault(event.getId(), 0L));
                    dto.setViews(viewsMap.getOrDefault(event.getId(), 0L));
                    return dto;
                })
                .collect(Collectors.toList());
    }


    public List<EventShortDto> enrichEventsShortDtoBatch(List<Event> events, EventMapper eventMapper) {
        if (events.isEmpty()) {
            return List.of();
        }

        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .collect(Collectors.toList());

        Map<Long, Long> confirmedRequestsMap = getConfirmedRequestsBatch(eventIds);
        Map<Long, Long> viewsMap = getViewsForEventsBatch(eventIds);

        return events.stream()
                .map(event -> {
                    EventShortDto dto = eventMapper.toShortDto(event);
                    dto.setConfirmedRequests(confirmedRequestsMap.getOrDefault(event.getId(), 0L));
                    dto.setViews(viewsMap.getOrDefault(event.getId(), 0L));
                    return dto;
                })
                .collect(Collectors.toList());
    }
}