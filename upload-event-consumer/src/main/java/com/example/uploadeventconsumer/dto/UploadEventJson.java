package com.example.uploadeventconsumer.dto;

import java.time.LocalDateTime;

public class UploadEventJson {
    private Long videoId;
    private String title;
    private String username;
    private String videoUrl;
    private LocalDateTime uploadedAt;
    private Double latitude;
    private Double longitude;

    public UploadEventJson() {
    }

    public UploadEventJson(Long videoId, String title, String username, String videoUrl, LocalDateTime uploadedAt, Double latitude, Double longitude) {
        this.videoId = videoId;
        this.title = title;
        this.username = username;
        this.videoUrl = videoUrl;
        this.uploadedAt = uploadedAt;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public Long getVideoId() {
        return videoId;
    }

    public void setVideoId(Long videoId) {
        this.videoId = videoId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }
}
