package com.example.jutjubic.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VideoPostResponse {
    private Long id;
    private String title;
    private String description;
    private Set<String> tags;
    private String videoUrl;
    private String transcodedVideoUrl;
    private String thumbnailPath;
    private LocalDateTime createdAt;
    private Long views;
    private Long likes;
    private Double longitude;
    private Double latitude;
    private String username;
    private boolean likedByCurrentUser;
    private LocalDateTime scheduledReleaseTime;
    private Long videoDurationSeconds;
}
