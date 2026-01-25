package ru.practicum.user.controller;

import ru.practicum.interaction.dto.user.NewUserRequest;
import ru.practicum.interaction.dto.user.UserResponse;
import ru.practicum.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(@Valid @RequestBody NewUserRequest userRequest) {
        return userService.createUser(userRequest);
    }

    @GetMapping
    public List<UserResponse> getUsers(@RequestParam(required = false) List<Long> ids,
                                       @RequestParam(defaultValue = "0") int from,
                                       @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(from / size, size);
        return userService.getUsers(ids, pageable);
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
    }
}