package com.example.jutjubic.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PopularVideosResponse {
    
    private LocalDateTime reportDate;
    private List<PopularVideoDto> popularVideos;
    
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PopularVideoDto {
        private Long id;
        private String title;
        private String description;
        private String thumbnailPath;
        private Double popularityScore;
        private Long totalViews;
        private Long likes;
        private String uploaderUsername;
    }
}
