package com.example.jutjubic.controller;

import com.example.jutjubic.dto.VideoPostResponse;
import com.example.jutjubic.service.VideoPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/map")
@RequiredArgsConstructor
public class MapController {
    private final VideoPostService videoPostService;

    @GetMapping
    public ResponseEntity<List<VideoPostResponse>> getAllVideosWithLocation() {
        return ResponseEntity.ok(videoPostService.getAllVideosWithLocation());
    }
}
