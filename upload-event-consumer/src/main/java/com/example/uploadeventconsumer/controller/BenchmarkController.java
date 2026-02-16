package com.example.uploadeventconsumer.controller;

import com.example.uploadeventconsumer.dto.BenchmarkResponse;
import com.example.uploadeventconsumer.dto.BenchmarkResult;
import com.example.uploadeventconsumer.service.BenchmarkService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/benchmark-results")
public class BenchmarkController {

    private final BenchmarkService benchmarkService;

    public BenchmarkController(BenchmarkService benchmarkService) {
        this.benchmarkService = benchmarkService;
    }

    @GetMapping
    public BenchmarkResponse getBenchmarkResults() {
        BenchmarkResult jsonResult = benchmarkService.getJsonBenchmark();
        BenchmarkResult protobufResult = benchmarkService.getProtobufBenchmark();
        
        BenchmarkResponse.ComparisonStats comparison = calculateComparison(jsonResult, protobufResult);
        
        return new BenchmarkResponse(jsonResult, protobufResult, comparison);
    }
    
    private BenchmarkResponse.ComparisonStats calculateComparison(BenchmarkResult json, BenchmarkResult protobuf) {
        if (json.getAvgDeserializeNs() == 0 || protobuf.getAvgDeserializeNs() == 0) {
            return new BenchmarkResponse.ComparisonStats(0, 0, 0, 0);
        }
        
        double speedRatio = Math.round((json.getAvgDeserializeNs() / protobuf.getAvgDeserializeNs()) * 100.0) / 100.0;
        double sizeRatio = Math.round((json.getAvgSizeBytes() / protobuf.getAvgSizeBytes()) * 100.0) / 100.0;
        
        double speedupPercent = Math.round((((json.getAvgDeserializeNs() - protobuf.getAvgDeserializeNs()) / json.getAvgDeserializeNs()) * 100.0) * 10.0) / 10.0;
        double compressionPercent = Math.round((((json.getAvgSizeBytes() - protobuf.getAvgSizeBytes()) / json.getAvgSizeBytes()) * 100.0) * 10.0) / 10.0;
        
        return new BenchmarkResponse.ComparisonStats(speedRatio, sizeRatio, speedupPercent, compressionPercent);
    }
}
