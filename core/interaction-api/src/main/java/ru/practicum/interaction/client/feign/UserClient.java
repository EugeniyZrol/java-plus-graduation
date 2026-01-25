package ru.practicum.interaction.client.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.interaction.dto.user.UserShortDto;

@FeignClient(name = "user-service")
public interface UserClient {
    @GetMapping("/users/feign/{userId}/exists")
    Boolean existsUserById(@PathVariable Long userId);

    @GetMapping("/users/feign/{userId}/short")
    UserShortDto getUserShortById(@PathVariable Long userId);
}