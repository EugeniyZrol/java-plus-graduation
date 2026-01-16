package ewm.user.mapper;

import ewm.user.dto.NewUserRequest;
import ewm.user.dto.UserResponse;
import ewm.user.model.User;
import ewm.user.dto.UserShortDto;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {
    User toEntity(NewUserRequest dto);

    UserResponse toDto(User user);

    UserShortDto toShortDto(User user);
}
