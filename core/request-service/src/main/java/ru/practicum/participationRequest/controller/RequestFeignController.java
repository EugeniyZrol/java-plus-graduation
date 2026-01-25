package ru.practicum.participationRequest.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.participationRequest.service.ParticipationRequestService;
import ru.practicum.participationRequest.service.RequestStatsService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/requests/feign")
@RequiredArgsConstructor
public class RequestFeignController {
    private final ParticipationRequestService requestService;
    private final RequestStatsService requestStatsService;

    @PostMapping("/events/confirmed/count/batch")
    public Map<Long, Integer> getConfirmedRequestsCountBatch(@RequestBody List<Long> eventIds) {
        Map<Long, Long> result = requestStatsService.getConfirmedRequestsBatch(eventIds);
        return result.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().intValue()));
    }

    @GetMapping("/events/{eventId}/confirmed/count")
    public Integer getConfirmedRequestsCount(@PathVariable Long eventId) {
        try {
            return requestService.getConfirmedRequestsCountForFeign(eventId);
        } catch (Exception e) {
            log.warn("Ошибка при получении подтвержденных запросов для события {}, возвращаем 0", eventId, e);
            return 0;
        }
    }
}