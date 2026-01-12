package ewm.user.service;

import ewm.user.dto.UserShortDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ewm.user.dto.NewUserRequest;
import ewm.user.dto.UserResponse;
import ewm.user.mapper.UserMapper;
import ewm.user.model.User;
import ewm.user.repository.UserRepository;
import ewm.exception.NotFoundException;
import ewm.exception.ConflictException;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public UserResponse createUser(NewUserRequest userRequest) {
        log.info("Создание пользователя с email: {}", userRequest.getEmail());

        if (userRepository.existsByEmail(userRequest.getEmail())) {
            log.warn("Попытка создания пользователя с существующим email: {}", userRequest.getEmail());
            throw new ConflictException("Пользователь с email уже существует: " + userRequest.getEmail());
        }

        User user = userMapper.toEntity(userRequest);
        User savedUser = userRepository.save(user);
        log.info("Пользователь создан успешно с ID: {}", savedUser.getId());
        return userMapper.toDto(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getUsers(List<Long> ids, Pageable pageable) {
        log.debug("Получение списка пользователей. IDs: {}, page: {}, size: {}",
                ids, pageable.getPageNumber(), pageable.getPageSize());

        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "id")
        );

        List<UserResponse> users;
        if (ids == null || ids.isEmpty()) {
            log.debug("Запрос всех пользователей с пагинацией");
            users = userRepository.findAll(sortedPageable)
                    .stream()
                    .map(userMapper::toDto)
                    .collect(Collectors.toList());
        } else {
            log.debug("Запрос пользователей по списку IDs: {}", ids);
            users = userRepository.findByIdIn(ids, sortedPageable)
                    .stream()
                    .map(userMapper::toDto)
                    .collect(Collectors.toList());
        }

        log.debug("Найдено пользователей: {}", users.size());
        return users;
    }

    @Override
    @Transactional(readOnly = true)
    public UserShortDto getUserById(Long userId) {
        log.debug("Получение пользователя по ID: {}", userId);
        checkUserExists(userId);
        UserShortDto user = userMapper.toShortDto(userRepository.findById(userId).get());
        log.debug("Пользователь с ID {} найден", userId);
        return user;
    }

    @Override
    public void deleteUser(Long userId) {
        log.info("Удаление пользователя с ID: {}", userId);
        checkUserExists(userId);
        userRepository.deleteById(userId);
        log.info("Пользователь с ID {} успешно удален", userId);
    }

    @Override
    public void checkUserExists(Long userId) {
        log.debug("Проверка существования пользователя с ID: {}", userId);
        if (!userRepository.existsById(userId)) {
            log.warn("Пользователь с ID {} не найден", userId);
            throw new NotFoundException("Пользователь не найден с id: " + userId);
        }
        log.debug("Пользователь с ID {} существует", userId);
    }

    @Override
    public User getUserEntityById(Long userId) {
        log.debug("Получение сущности пользователя по ID: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("Сущность пользователя с ID {} не найдена", userId);
                    return new NotFoundException("Пользователь не найден с id: " + userId);
                });
        log.debug("Сущность пользователя с ID {} получена", userId);
        return user;
    }
}