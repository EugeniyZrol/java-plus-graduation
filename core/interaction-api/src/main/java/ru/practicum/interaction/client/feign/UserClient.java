package ru.practicum.interaction.client.feign;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.interaction.client.fallback.UserClientFallback;
import ru.practicum.interaction.dto.user.UserShortDto;

@FeignClient(name = "user-service", fallback = UserClientFallback.class)
public interface UserClient {

    @GetMapping("/users/feign/{userId}/exists")
    @CircuitBreaker(name = "userServiceCircuitBreaker")
    Boolean existsUserById(@PathVariable Long userId);

    @GetMapping("/users/feign/{userId}/short")
    @CircuitBreaker(name = "userServiceCircuitBreaker")
    UserShortDto getUserShortById(@PathVariable Long userId);
}