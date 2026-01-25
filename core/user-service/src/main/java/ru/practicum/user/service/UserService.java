package ru.practicum.user.service;

import ru.practicum.interaction.dto.user.NewUserRequest;
import ru.practicum.interaction.dto.user.UserResponse;
import ru.practicum.interaction.dto.user.UserShortDto;
import ru.practicum.user.model.User;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserService {
    UserResponse createUser(NewUserRequest userRequest);

    List<UserResponse> getUsers(List<Long> ids, Pageable pageable);

    void deleteUser(Long userId);

    UserResponse getUserDtoById(Long userId);

    UserShortDto getUserShortDtoById(Long userId);

    Boolean existsById(Long userId);

    User getUserEntityById(Long userId);

    void checkUserExists(Long userId);
}