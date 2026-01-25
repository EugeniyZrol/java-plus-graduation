package ru.practicum.interaction.client.fallback;


import org.springframework.stereotype.Component;
import ru.practicum.interaction.client.feign.UserClient;
import ru.practicum.interaction.dto.user.UserShortDto;

@Component
public class UserClientFallback implements UserClient {
    @Override public Boolean existsUserById(Long userId) { return true; }
    @Override public UserShortDto getUserShortById(Long userId) {
        return UserShortDto.builder().id(userId).name("User " + userId).build();
    }
}