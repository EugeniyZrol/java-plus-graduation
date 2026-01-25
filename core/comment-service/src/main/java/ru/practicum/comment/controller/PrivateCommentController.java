package ru.practicum.comment.controller;

import org.springframework.data.domain.Pageable;
import ru.practicum.interaction.dto.comment.CommentDto;
import ru.practicum.interaction.dto.comment.NewCommentDto;
import ru.practicum.interaction.dto.comment.UpdateCommentDto;
import ru.practicum.comment.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users/{userId}/comments")
@RequiredArgsConstructor
public class PrivateCommentController {
    private final CommentService commentService;

    @PostMapping("/event/{eventId}")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentDto createComment(@PathVariable Long userId,
                                    @PathVariable Long eventId,
                                    @Valid @RequestBody NewCommentDto newCommentDto) {
        return commentService.createComment(userId, eventId, newCommentDto);
    }

    @PatchMapping("/{commentId}")
    public CommentDto updateComment(@PathVariable Long userId,
                                    @PathVariable Long commentId,
                                    @Valid @RequestBody UpdateCommentDto updateCommentDto) {
        return commentService.updateComment(userId, commentId, updateCommentDto);
    }

    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable Long userId,
                              @PathVariable Long commentId) {
        commentService.deleteComment(userId, commentId);
    }

    @GetMapping
    public List<CommentDto> getCommentsByUser(@PathVariable Long userId,
                                              @RequestParam(defaultValue = "0") int from,
                                              @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(from / size, size);
        return commentService.getCommentsByUser(userId, pageable);
    }

    @GetMapping("/event/{eventId}/list")
    public List<CommentDto> getCommentsForEventByUser(
            @PathVariable Long userId,
            @PathVariable Long eventId,
            @RequestParam(defaultValue = "0") int from,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(from / size, size);
        return commentService.getCommentsByEvent(eventId, pageable);
    }
}
