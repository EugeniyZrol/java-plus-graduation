package ru.practicum.interaction.client.feign;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.interaction.dto.event.EventFullDto;
import ru.practicum.interaction.client.fallback.EventClientFallback;

@FeignClient(name = "event-service", fallback = EventClientFallback.class)
public interface EventClient {
    @GetMapping("/events/feign/{eventId}/exists")
    @CircuitBreaker(name = "eventServiceCircuitBreaker")
    Boolean existsEventById(@PathVariable Long eventId);

    @GetMapping("/events/feign/{eventId}/state")
    @CircuitBreaker(name = "eventServiceCircuitBreaker")
    String getEventState(@PathVariable Long eventId);


    @GetMapping("/events/feign/{eventId}")
    @CircuitBreaker(name = "eventServiceCircuitBreaker")
    EventFullDto getEventById(@PathVariable Long eventId);
}