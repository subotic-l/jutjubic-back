package com.example.jutjubic.controller;

import com.example.jutjubic.dto.LikeResponse;
import com.example.jutjubic.dto.VideoPostRequest;
import com.example.jutjubic.dto.VideoPostResponse;
import com.example.jutjubic.model.User;
import com.example.jutjubic.service.UserService;
import com.example.jutjubic.service.VideoPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
public class VideoPostController {

    private final VideoPostService videoPostService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<VideoPostResponse>> getAllVideos() {
        return ResponseEntity.ok(videoPostService.getAllVideos());
    }

    @GetMapping("/{id:[0-9]+}")
    public ResponseEntity<VideoPostResponse> getVideoById(@PathVariable Long id) {
        return ResponseEntity.ok(videoPostService.getVideoById(id));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<VideoPostResponse> createVideoPost(@ModelAttribute VideoPostRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.findByEmail(email);
        try {
            VideoPostResponse response = videoPostService.createVideoPost(request, user);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<LikeResponse> toggleLike(@PathVariable Long id) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.findByEmail(email);
        LikeResponse response = videoPostService.toggleLike(id, user);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/thumbnails/{thumbnailPath:.+}")
    public ResponseEntity<byte[]> getThumbnail(@PathVariable String thumbnailPath) {
        try {
            byte[] thumbnail = videoPostService.getThumbnail("uploads/thumbnails/" + thumbnailPath);
            
            String contentType = "image/jpeg";
            if (thumbnailPath.toLowerCase().endsWith(".png")) {
                contentType = "image/png";
            } else if (thumbnailPath.toLowerCase().endsWith(".gif")) {
                contentType = "image/gif";
            }
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(thumbnail);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/videos/{videoPath:.+}")
    public ResponseEntity<byte[]> getVideo(@PathVariable String videoPath) {
        try {
            byte[] video = videoPostService.getVideo("uploads/videos/" + videoPath);
            
            String contentType = "video/mp4";
            if (videoPath.toLowerCase().endsWith(".webm")) {
                contentType = "video/webm";
            } else if (videoPath.toLowerCase().endsWith(".ogg")) {
                contentType = "video/ogg";
            } else if (videoPath.toLowerCase().endsWith(".avi")) {
                contentType = "video/x-msvideo";
            }
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(video);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
