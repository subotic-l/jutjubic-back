package com.example.jutjubic.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VideoPostRequest {
    private String title;
    private String description;
    private Set<String> tags;
    private MultipartFile thumbnail;
    private MultipartFile video;
    private String location;
}
