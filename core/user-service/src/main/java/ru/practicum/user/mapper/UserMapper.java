package ru.practicum.user.mapper;

import ru.practicum.interaction.dto.user.NewUserRequest;
import ru.practicum.interaction.dto.user.UserResponse;
import ru.practicum.user.model.User;
import ru.practicum.interaction.dto.user.UserShortDto;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {
    User toEntity(NewUserRequest dto);

    UserResponse toDto(User user);

    UserShortDto toShortDto(User user);
}