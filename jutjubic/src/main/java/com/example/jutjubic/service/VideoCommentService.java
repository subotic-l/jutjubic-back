package com.example.jutjubic.service;

import com.example.jutjubic.dto.VideoCommentRequest;
import com.example.jutjubic.dto.VideoCommentResponse;
import com.example.jutjubic.model.User;
import com.example.jutjubic.model.VideoComment;
import com.example.jutjubic.model.VideoPost;
import com.example.jutjubic.repository.VideoCommentRepository;
import com.example.jutjubic.repository.VideoPostRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VideoCommentService {

    private final VideoCommentRepository commentRepository;
    private final VideoPostRepository videoPostRepository;

    @CircuitBreaker(name = "database")
    @Retry(name = "database")
    @Transactional
    public VideoCommentResponse addComment(VideoCommentRequest request, User user) {
        VideoPost videoPost = videoPostRepository.findById(request.getVideoId())
                .orElseThrow(() -> new RuntimeException("Video not found"));

        VideoComment comment = new VideoComment();
        comment.setVideoPost(videoPost);
        comment.setUser(user);
        comment.setText(request.getText());

        comment = commentRepository.save(comment);

        return mapToResponse(comment);
    }

    @CircuitBreaker(name = "database")
    @Retry(name = "database")
    @Cacheable(value = "videoComments", key = "#videoId + '-' + #page + '-' + #size")
    public List<VideoCommentResponse> getComments(Long videoId, int page, int size) {
        VideoPost videoPost = videoPostRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found"));

        Page<VideoComment> commentPage = commentRepository
                .findByVideoPostOrderByCreatedAtDesc(videoPost, PageRequest.of(page, size));

        return commentPage.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private VideoCommentResponse mapToResponse(VideoComment comment) {
        VideoCommentResponse response = new VideoCommentResponse();
        response.setId(comment.getId());
        response.setText(comment.getText());
        response.setUsername(comment.getUser().getActualUsername());
        response.setCreatedAt(comment.getCreatedAt());
        return response;
    }
}
