package ewm.comment.service;

import ewm.comment.dto.CommentDto;
import ewm.comment.dto.NewCommentDto;
import ewm.comment.dto.UpdateCommentDto;
import ewm.comment.mapper.CommentMapper;
import ewm.comment.model.Comment;
import ewm.comment.repository.CommentRepository;
import ewm.event.model.Event;
import ewm.event.repository.EventRepository;
import ewm.exception.ConflictException;
import ewm.exception.NotFoundException;
import ewm.user.model.User;
import ewm.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final EventRepository eventRepository;
    private final UserService userService;
    private final CommentMapper commentMapper;

    @Override
    @Transactional
    public CommentDto createComment(Long userId, Long eventId, NewCommentDto newCommentDto) {
        log.debug("Создание нового комментария: userId={}, eventId={}", userId, eventId);

        User author = userService.getUserEntityById(userId);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> {
                    log.warn("Попытка создания комментария к несуществующему событию: eventId={}", eventId);
                    return new NotFoundException("Событие не найдено");
                });

        if (!"PUBLISHED".equals(event.getState())) {
            log.warn("Попытка создания комментария к неопубликованному событию: eventId={}, state={}",
                    eventId, event.getState());
            throw new ConflictException("Невозможно прокомментировать неопубликованное событие");
        }

        Comment comment = commentMapper.toComment(newCommentDto, author, event);
        Comment savedComment = commentRepository.save(comment);

        log.info("Создан новый комментарий: ID={}, authorId={}, eventId={}",
                savedComment.getId(), userId, eventId);

        return commentMapper.toDto(savedComment);
    }

    @Override
    @Transactional
    public CommentDto updateComment(Long userId, Long commentId, UpdateCommentDto updateCommentDto) {
        log.debug("Обновление комментария: userId={}, commentId={}", userId, commentId);

        Comment comment = commentRepository.findByIdAndAuthorId(commentId, userId)
                .orElseThrow(() -> {
                    log.warn("Попытка обновления несуществующего комментария: commentId={}, userId={}",
                            commentId, userId);
                    return new NotFoundException("Комментарий не найден");
                });

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

        Comment comment = commentRepository.findByIdAndAuthorId(commentId, userId)
                .orElseThrow(() -> {
                    log.warn("Попытка удаления несуществующего комментария: commentId={}, userId={}",
                            commentId, userId);
                    return new NotFoundException("Комментарий не найден");
                });

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
        log.debug("Получение комментариев для события: eventId={}, page={}, size={}",
                eventId, pageable.getPageNumber(), pageable.getPageSize());

        if (!eventRepository.existsById(eventId)) {
            log.warn("Попытка получения комментариев для несуществующего события: eventId={}", eventId);
            throw new NotFoundException("Событие не найдено");
        }

        return commentRepository.findByEventIdNotDeleted(eventId, pageable)
                .stream()
                .map(commentMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentDto> getCommentsByUser(Long userId, Pageable pageable) {
        log.debug("Получение комментариев пользователя: userId={}, page={}, size={}",
                userId, pageable.getPageNumber(), pageable.getPageSize());

        userService.getUserById(userId);

        return commentRepository.findByAuthorIdAndIsDeletedFalse(userId, pageable)
                .stream()
                .map(commentMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CommentDto getCommentById(Long commentId) {
        log.debug("Получение комментария по ID: commentId={}", commentId);

        Comment comment = commentRepository.findByIdNotDeleted(commentId)
                .orElseThrow(() -> {
                    log.warn("Попытка получения несуществующего комментария: commentId={}", commentId);
                    return new NotFoundException("Комментарий не найден");
                });

        log.debug("Комментарий найден: commentId={}", commentId);
        return commentMapper.toDto(comment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentDto> getCommentsForEvents(List<Long> eventIds) {
        log.debug("Получение комментариев для списка событий: eventsCount={}", eventIds.size());

        List<Comment> comments = commentRepository.findByEventIds(eventIds);
        List<CommentDto> result = comments.stream()
                .map(commentMapper::toDto)
                .collect(Collectors.toList());

        log.debug("Найдено комментариев для {} событий: {}", eventIds.size(), result.size());
        return result;
    }
}