package com.example.jutjubic.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VideoCommentRequest {
    private String text;
    private Long videoId;
}