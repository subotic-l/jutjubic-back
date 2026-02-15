package com.example.uploadeventconsumer.controller;

import com.example.uploadeventconsumer.dto.BenchmarkResponse;
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
        return new BenchmarkResponse(
                benchmarkService.getJsonBenchmark(),
                benchmarkService.getProtobufBenchmark()
        );
    }
}
