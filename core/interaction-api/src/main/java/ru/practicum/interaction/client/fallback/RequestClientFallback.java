package ru.practicum.interaction.client.fallback;

import org.springframework.stereotype.Component;
import ru.practicum.interaction.client.feign.RequestClient;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class RequestClientFallback implements RequestClient {
    @Override
    public Map<Long, Integer> getConfirmedRequestsCountBatch(List<Long> eventIds) {
        return eventIds.stream().collect(Collectors.toMap(id -> id, id -> 0));
    }
}