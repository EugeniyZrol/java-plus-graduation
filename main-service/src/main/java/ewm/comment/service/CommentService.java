package ewm.comment.service;

import ewm.comment.dto.CommentDto;
import ewm.comment.dto.NewCommentDto;
import ewm.comment.dto.UpdateCommentDto;
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

    List<CommentDto> getCommentsForEvents(List<Long> eventIds);
}