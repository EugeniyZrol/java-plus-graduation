package ru.practicum.comment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.interaction.client.feign.EventClient;
import ru.practicum.interaction.client.feign.UserClient;
import ru.practicum.comment.mapper.CommentMapper;
import ru.practicum.comment.model.Comment;
import ru.practicum.comment.repository.CommentRepository;
import ru.practicum.interaction.dto.comment.CommentDto;
import ru.practicum.interaction.dto.comment.NewCommentDto;
import ru.practicum.interaction.dto.comment.UpdateCommentDto;
import ru.practicum.interaction.dto.user.UserShortDto;
import ru.practicum.interaction.exception.ConflictException;
import ru.practicum.interaction.exception.NotFoundException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final CommentMapper commentMapper;
    private final UserClient userClient;
    private final EventClient eventClient;

    @Override
    @Transactional
    public CommentDto createComment(Long userId, Long eventId, NewCommentDto newCommentDto) {
        log.debug("Создание нового комментария: userId={}, eventId={}", userId, eventId);

        Boolean userExists = userClient.existsUserById(userId);
        if (userExists == null || !userExists) {
            log.warn("Попытка создания комментария несуществующим пользователем: userId={}", userId);
            throw new NotFoundException("Пользователь не найден");
        }

        Boolean eventExists = eventClient.existsEventById(eventId);
        if (eventExists == null || !eventExists) {
            log.warn("Попытка создания комментария к несуществующему событию: eventId={}", eventId);
            throw new NotFoundException("Событие не найдено");
        }

        String eventState = eventClient.getEventState(eventId);
        if (eventState == null || !"PUBLISHED".equals(eventState)) {
            log.warn("Попытка создания комментария к неопубликованному событию: eventId={}", eventId);
            throw new ConflictException("Невозможно прокомментировать неопубликованное событие");
        }

        Comment comment = commentMapper.toComment(newCommentDto);
        comment.setAuthorId(userId);
        comment.setEventId(eventId);

        Comment savedComment = commentRepository.save(comment);

        log.info("Создан новый комментарий: ID={}, authorId={}, eventId={}",
                savedComment.getId(), userId, eventId);

        return commentMapper.toDto(savedComment);
    }

    @Override
    @Transactional
    public CommentDto updateComment(Long userId, Long commentId, UpdateCommentDto updateCommentDto) {
        log.debug("Обновление комментария: userId={}, commentId={}", userId, commentId);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> {
                    log.warn("Попытка обновления несуществующего комментария: commentId={}", commentId);
                    return new NotFoundException("Комментарий не найден");
                });

        if (!comment.getAuthorId().equals(userId)) {
            log.warn("Пользователь {} пытается обновить чужой комментарий {}", userId, commentId);
            throw new NotFoundException("Комментарий не найден");
        }

        if (comment.getIsDeleted()) {
            log.warn("Попытка обновления удаленного комментария: commentId={}", commentId);
            throw new ConflictException("Не удается обновить удаленный комментарий");
        }

        commentMapper.updateCommentFromDto(updateCommentDto, comment);
        comment.setIsEdited(true);

        Comment updatedComment = commentRepository.save(comment);
        log.info("Комментарий обновлен: commentId={}", commentId);

        return commentMapper.toDto(updatedComment);
    }

    @Override
    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        log.debug("Удаление комментария пользователем: userId={}, commentId={}", userId, commentId);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> {
                    log.warn("Попытка удаления несуществующего комментария: commentId={}", commentId);
                    return new NotFoundException("Комментарий не найден");
                });

        if (!comment.getAuthorId().equals(userId)) {
            log.warn("Пользователь {} пытается удалить чужой комментарий {}", userId, commentId);
            throw new NotFoundException("Комментарий не найден");
        }

        comment.setIsDeleted(true);
        commentRepository.save(comment);

        log.info("Комментарий удален пользователем: commentId={}, userId={}", commentId, userId);
    }

    @Override
    @Transactional
    public void deleteCommentByAdmin(Long commentId) {
        log.debug("Удаление комментария администратором: commentId={}", commentId);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> {
                    log.warn("Попытка удаления несуществующего комментария администратором: commentId={}", commentId);
                    return new NotFoundException("Комментарий не найден");
                });

        comment.setIsDeleted(true);
        commentRepository.save(comment);
        log.info("Комментарий удален администратором: commentId={}", commentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentDto> getCommentsByEvent(Long eventId, Pageable pageable) {
        log.debug("Получение комментариев для события: eventId={}", eventId);

        Boolean eventExists = eventClient.existsEventById(eventId);
        if (eventExists == null || !eventExists) {
            log.warn("Попытка получения комментариев для несуществующего события: eventId={}", eventId);
            throw new NotFoundException("Событие не найдено");
        }

        return commentRepository.findByEventIdAndIsDeletedFalse(eventId, pageable)
                .stream()
                .map(commentMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentDto> getCommentsByUser(Long userId, Pageable pageable) {
        log.debug("Получение комментариев пользователя: userId={}", userId);

        Boolean userExists = userClient.existsUserById(userId);
        if (userExists == null || !userExists) {
            log.warn("Попытка получения комментариев несуществующего пользователя: userId={}", userId);
            throw new NotFoundException("Пользователь не найден");
        }

        return commentRepository.findByAuthorIdAndIsDeletedFalse(userId, pageable)
                .stream()
                .map(comment -> {
                    CommentDto dto = commentMapper.toDto(comment);
                    UserShortDto author = userClient.getUserShortById(userId);
                    dto.setAuthor(author);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CommentDto getCommentById(Long commentId) {
        log.debug("Получение комментария по ID: commentId={}", commentId);

        Comment comment = commentRepository.findByIdAndIsDeletedFalse(commentId)
                .orElseThrow(() -> {
                    log.warn("Попытка получения несуществующего комментария: commentId={}", commentId);
                    return new NotFoundException("Комментарий не найден");
                });

        CommentDto dto = commentMapper.toDto(comment);
        UserShortDto author = userClient.getUserShortById(comment.getAuthorId());
        dto.setAuthor(author);

        log.debug("Комментарий найден: commentId={}", commentId);
        return dto;
    }

    @Override
    public List<CommentDto> getCommentsByEventId(Long eventId) {
        log.debug("Feign: получение комментариев события: eventId={}", eventId);
        return commentRepository.findByEventIdAndIsDeletedFalse(eventId)
                .stream()
                .map(commentMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public Integer getCommentsCountByEventId(Long eventId) {
        log.debug("Feign: подсчет комментариев события: eventId={}", eventId);
        return commentRepository.countByEventIdAndIsDeletedFalse(eventId);
    }

    @Override
    public Boolean existsById(Long commentId) {
        log.debug("Feign: проверка существования комментария: commentId={}", commentId);
        return commentRepository.existsByIdAndIsDeletedFalse(commentId);
    }

    @Override
    public Boolean isUserCommentAuthor(Long userId, Long commentId) {
        log.debug("Feign: проверка авторства комментария: userId={}, commentId={}", userId, commentId);
        return commentRepository.existsByIdAndAuthorIdAndIsDeletedFalse(commentId, userId);
    }
}