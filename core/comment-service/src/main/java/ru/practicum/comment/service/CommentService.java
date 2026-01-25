package ru.practicum.comment.service;

import ru.practicum.interaction.dto.comment.CommentDto;
import ru.practicum.interaction.dto.comment.NewCommentDto;
import ru.practicum.interaction.dto.comment.UpdateCommentDto;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CommentService {
    CommentDto createComment(Long userId, Long eventId, NewCommentDto newCommentDto);

    CommentDto updateComment(Long userId, Long commentId, UpdateCommentDto updateCommentDto);

    void deleteComment(Long userId, Long commentId);

    void deleteCommentByAdmin(Long commentId);

    List<CommentDto> getCommentsByEvent(Long eventId, Pageable pageable);

    List<CommentDto> getCommentsByUser(Long userId, Pageable pageable);

    CommentDto getCommentById(Long commentId);

    List<CommentDto> getCommentsByEventId(Long eventId);

    Integer getCommentsCountByEventId(Long eventId);

    Boolean existsById(Long commentId);

    Boolean isUserCommentAuthor(Long userId, Long commentId);
}