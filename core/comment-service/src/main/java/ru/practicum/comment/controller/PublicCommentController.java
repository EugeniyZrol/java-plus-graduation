package ru.practicum.comment.controller;

import org.springframework.data.domain.Pageable;
import ru.practicum.interaction.dto.comment.CommentDto;
import ru.practicum.comment.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
public class PublicCommentController {
    private final CommentService commentService;

    @GetMapping("/event/{eventId}")
    public List<CommentDto> getCommentsByEvent(
            @PathVariable Long eventId,
            @RequestParam(defaultValue = "0") int from,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(from / size, size);
        return commentService.getCommentsByEvent(eventId, pageable);
    }

    @GetMapping("/{commentId}")
    public CommentDto getComment(@PathVariable Long commentId) {
        return commentService.getCommentById(commentId);
    }
}
