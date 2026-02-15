package com.example.uploadeventconsumer.dto;

public class UploadEventJson {
    private String title;
    private String author;
    private long size;
    private long duration;
    private long uploadedAt;

    public UploadEventJson() {
    }

    public UploadEventJson(String title, String author, long size, long duration, long uploadedAt) {
        this.title = title;
        this.author = author;
        this.size = size;
        this.duration = duration;
        this.uploadedAt = uploadedAt;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public long getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(long uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
}
