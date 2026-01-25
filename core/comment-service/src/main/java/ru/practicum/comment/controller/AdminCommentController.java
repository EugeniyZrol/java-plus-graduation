package ru.practicum.comment.controller;

import ru.practicum.comment.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/comments")
@RequiredArgsConstructor
public class AdminCommentController {
    private final CommentService commentService;

    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable Long commentId) {
        commentService.deleteCommentByAdmin(commentId);
    }
}