package com.example.jutjubic.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VideoCommentResponse {
    private Long id;
    private String text;
    private String username;
    private LocalDateTime createdAt;
}