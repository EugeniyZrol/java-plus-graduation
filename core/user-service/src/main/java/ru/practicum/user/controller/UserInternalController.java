package ru.practicum.user.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.interaction.dto.user.UserResponse;
import ru.practicum.interaction.dto.user.UserShortDto;
import ru.practicum.user.service.UserService;

@RestController
@RequestMapping("/users/feign")
@RequiredArgsConstructor
public class UserInternalController {
    private final UserService userService;

    @GetMapping("/{userId}/exists")
    public Boolean existsUserById(@PathVariable Long userId) {
        return userService.existsById(userId);
    }

    @GetMapping("/{userId}")
    public UserResponse getUserById(@PathVariable Long userId) {
        return userService.getUserDtoById(userId);
    }

    @GetMapping("/{userId}/short")
    public UserShortDto getUserShortById(@PathVariable Long userId) {
        return userService.getUserShortDtoById(userId);
    }
}