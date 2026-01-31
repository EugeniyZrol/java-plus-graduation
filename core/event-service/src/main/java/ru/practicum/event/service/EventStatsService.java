package ru.practicum.event.service;

import ru.practicum.interaction.client.feign.RequestClient;
import ru.practicum.interaction.dto.categories.CategoryDto;
import ru.practicum.interaction.dto.event.EventFullDto;
import ru.practicum.interaction.dto.event.EventShortDto;
import ru.practicum.event.mapper.EventMapper;
import ru.practicum.event.model.Event;
import ru.practicum.ewm.stats.client.AnalyzerGrpcClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.interaction.dto.user.UserShortDto;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventStatsService {
    private final RequestClient requestClient;
    private final EventMapper eventMapper;
    private final AnalyzerGrpcClient analyzerGrpcClient;

    public EventFullDto enrichEventFullDto(Event event, CategoryDto category, UserShortDto initiator) {
        EventFullDto dto = eventMapper.toFullDto(event, category, initiator);

        Long confirmedRequests = getConfirmedRequests(event.getId());
        dto.setConfirmedRequests(confirmedRequests);

        Double rating = analyzerGrpcClient.getEventRating(event.getId());
        dto.setRating(rating != null ? rating : 0.0);

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

        Map<Long, Double> ratingsMap = getRatingsForEventsBatch(eventIds);
        Map<Long, Long> requestsMap = getConfirmedRequestsBatch(eventIds);

        return events.stream()
                .map(event -> {
                    EventShortDto dto = eventMapper.toShortDto(
                            event,
                            categories.get(event.getCategoryId()),
                            users.get(event.getInitiatorId())
                    );
                    dto.setRating(ratingsMap.getOrDefault(event.getId(), 0.0));
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

        Map<Long, Double> ratingsMap = getRatingsForEventsBatch(eventIds);
        Map<Long, Long> requestsMap = getConfirmedRequestsBatch(eventIds);

        return events.stream()
                .map(event -> {
                    EventFullDto dto = eventMapper.toFullDto(
                            event,
                            categories.get(event.getCategoryId()),
                            users.get(event.getInitiatorId())
                    );
                    dto.setRating(ratingsMap.getOrDefault(event.getId(), 0.0));
                    dto.setConfirmedRequests(requestsMap.getOrDefault(event.getId(), 0L));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    public Map<Long, Double> getRatingsForEventsBatch(List<Long> eventIds) {
        if (eventIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, Double> ratings = new HashMap<>();
        for (Long eventId : eventIds) {
            Double rating = analyzerGrpcClient.getEventRating(eventId);
            ratings.put(eventId, rating != null ? rating : 0.0);
        }
        return ratings;
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
}