package com.example.jutjubic.controller;

import com.example.jutjubic.dto.MapBoundsRequest;
import com.example.jutjubic.dto.VideoPostResponse;
import com.example.jutjubic.service.VideoPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/viewport")
    public ResponseEntity<List<VideoPostResponse>> getVideosInViewport(
            @RequestParam Double minLat,
            @RequestParam Double maxLat,
            @RequestParam Double minLon,
            @RequestParam Double maxLon) {
        
        List<VideoPostResponse> videos = videoPostService.getVideosWithinBounds(
            minLat, maxLat, minLon, maxLon
        );
        return ResponseEntity.ok(videos);
    }

    @PostMapping("/viewport")
    public ResponseEntity<List<VideoPostResponse>> getVideosInViewportPost(
            @RequestBody MapBoundsRequest bounds) {
        
        List<VideoPostResponse> videos = videoPostService.getVideosWithinBounds(
            bounds.getMinLatitude(),
            bounds.getMaxLatitude(),
            bounds.getMinLongitude(),
            bounds.getMaxLongitude()
        );
        return ResponseEntity.ok(videos);
    }
}
