package com.example.jutjubic.controller;

import com.example.jutjubic.dto.TileRequest;
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

    @PostMapping("/tiles")
    public ResponseEntity<List<VideoPostResponse>> getVideosForTiles(@RequestBody TileRequest request) {
        List<VideoPostResponse> videos = videoPostService.getVideosForTiles(request.getTiles());
        return ResponseEntity.ok(videos);
    }
}
