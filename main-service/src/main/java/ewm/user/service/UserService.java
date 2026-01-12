package ewm.user.service;

import ewm.user.dto.NewUserRequest;
import ewm.user.dto.UserResponse;
import ewm.user.dto.UserShortDto;
import ewm.user.model.User;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserService {
    UserResponse createUser(NewUserRequest userRequest);

    List<UserResponse> getUsers(List<Long> ids, Pageable pageable);

    UserShortDto getUserById(Long userId);

    void deleteUser(Long userId);

    void checkUserExists(Long userId);

    User getUserEntityById(Long userId);
}