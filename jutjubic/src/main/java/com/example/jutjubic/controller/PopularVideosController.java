package com.example.jutjubic.controller;

import com.example.jutjubic.dto.PopularVideosResponse;
import com.example.jutjubic.service.PopularVideosService;
import com.example.jutjubic.service.VideoPopularityETLService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/popular-videos")
@RequiredArgsConstructor
public class PopularVideosController {

    private final PopularVideosService popularVideosService;
    private final VideoPopularityETLService etlService;

    @GetMapping
    public ResponseEntity<PopularVideosResponse> getPopularVideos() {
        return popularVideosService.getPopularVideos()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @PostMapping("/run-etl")
    public ResponseEntity<String> runETLPipeline() {
        etlService.runETLPipelineManually();
        return ResponseEntity.ok("ETL pipeline executed successfully");
    }
}
