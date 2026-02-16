package com.example.jutjubic.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadEventDto {
    private Long videoId;
    private String title;
    private String username;
    private String videoUrl;
    private LocalDateTime uploadedAt;
    private Double latitude;
    private Double longitude;
}
