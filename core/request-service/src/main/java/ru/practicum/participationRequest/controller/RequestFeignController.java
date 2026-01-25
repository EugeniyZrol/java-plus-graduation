package ru.practicum.participationRequest.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.participationRequest.service.ParticipationRequestService;
import ru.practicum.participationRequest.service.RequestStatsService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/requests/feign")
@RequiredArgsConstructor
public class RequestFeignController {
    private final ParticipationRequestService requestService;
    private final RequestStatsService requestStatsService;

    @GetMapping("/events/{eventId}/confirmed/count")
    public Integer getConfirmedRequestsCount(@PathVariable Long eventId) {
        return requestService.getConfirmedRequestsCountForFeign(eventId);
    }

    @PostMapping("/events/confirmed/count/batch")
    public Map<Long, Integer> getConfirmedRequestsCountBatch(@RequestBody List<Long> eventIds) {
        Map<Long, Long> result = requestStatsService.getConfirmedRequestsBatch(eventIds);
        return result.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().intValue()));
    }
}