package com.example.jutjubic.controller;

import com.example.jutjubic.dto.VideoCommentRequest;
import com.example.jutjubic.dto.VideoCommentResponse;
import com.example.jutjubic.model.User;
import com.example.jutjubic.service.UserService;
import com.example.jutjubic.service.VideoCommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class VideoCommentController {

    private final VideoCommentService commentService;
    private final UserService userService;

    @PostMapping
    public VideoCommentResponse addComment(@RequestBody VideoCommentRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.findByEmail(email);
        return commentService.addComment(request, user);
    }

    @GetMapping("/{videoId}")
    public List<VideoCommentResponse> getComments(
            @PathVariable Long videoId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return commentService.getComments(videoId, page, size);
    }
}
