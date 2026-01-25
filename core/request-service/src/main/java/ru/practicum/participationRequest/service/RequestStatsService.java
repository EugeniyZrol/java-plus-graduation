package ru.practicum.participationRequest.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.participationRequest.repository.ParticipationRequestRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class RequestStatsService {

    private final ParticipationRequestRepository requestRepository;

    public Long getConfirmedRequestsCount(Long eventId) {
        return requestRepository.countConfirmedRequestsByEventId(eventId);
    }

    public Map<Long, Long> getConfirmedRequestsBatch(List<Long> eventIds) {
        Map<Long, Long> result = new HashMap<>();
        List<Object[]> counts = requestRepository.countConfirmedRequestsByEventIds(eventIds);

        for (Object[] row : counts) {
            Long eventId = (Long) row[0];
            Long count = (Long) row[1];
            result.put(eventId, count);
        }

        for (Long eventId : eventIds) {
            result.putIfAbsent(eventId, 0L);
        }

        return result;
    }
}