package ru.practicum.interaction.client.feign;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.practicum.interaction.client.fallback.RequestClientFallback;

import java.util.List;
import java.util.Map;

@FeignClient(name = "request-service", fallback = RequestClientFallback.class)
public interface RequestClient {

    @PostMapping("/requests/feign/events/confirmed/count/batch")
    @CircuitBreaker(name = "requestServiceCircuitBreaker")
    Map<Long, Integer> getConfirmedRequestsCountBatch(@RequestBody List<Long> eventIds);
}