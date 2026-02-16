package com.example.jutjubic.dto;

import lombok.Data;

@Data
public class VideoCommentRequest {
    private String text;
    private Long videoId;
}