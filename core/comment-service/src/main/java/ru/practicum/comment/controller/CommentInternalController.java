package ru.practicum.comment.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.comment.service.CommentService;
import ru.practicum.interaction.dto.comment.CommentDto;

import java.util.List;

@RestController
@RequestMapping("/comments/feign")
@RequiredArgsConstructor
public class CommentInternalController {
    private final CommentService commentService;

    @GetMapping("/events/{eventId}/exists")
    public Boolean existsCommentByEventId(@PathVariable Long eventId) {
        return commentService.getCommentsCountByEventId(eventId) > 0;
    }

    @GetMapping("/events/{eventId}/count")
    public Integer getCommentsCountByEventId(@PathVariable Long eventId) {
        return commentService.getCommentsCountByEventId(eventId);
    }

    @GetMapping("/events/{eventId}")
    public List<CommentDto> getCommentsByEventId(@PathVariable Long eventId) {
        return commentService.getCommentsByEventId(eventId);
    }

    @GetMapping("/{commentId}/exists")
    public Boolean existsCommentById(@PathVariable Long commentId) {
        return commentService.existsById(commentId);
    }

    @GetMapping("/{commentId}/author/{userId}")
    public Boolean isUserCommentAuthor(@PathVariable Long userId,
                                       @PathVariable Long commentId) {
        return commentService.isUserCommentAuthor(userId, commentId);
    }
}