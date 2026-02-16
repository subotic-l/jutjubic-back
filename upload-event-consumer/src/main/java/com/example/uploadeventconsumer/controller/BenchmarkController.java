package com.example.uploadeventconsumer.controller;

import com.example.uploadeventconsumer.dto.BenchmarkResponse;
import com.example.uploadeventconsumer.dto.BenchmarkResult;
import com.example.uploadeventconsumer.dto.DetailedBenchmarkResponse;
import com.example.uploadeventconsumer.dto.DetailedBenchmarkResult;
import com.example.uploadeventconsumer.service.BenchmarkService;
import com.example.uploadeventconsumer.service.IsolatedBenchmarkService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/benchmark-results")
public class BenchmarkController {

    private final BenchmarkService benchmarkService;
    private final IsolatedBenchmarkService isolatedBenchmarkService;

    public BenchmarkController(BenchmarkService benchmarkService, IsolatedBenchmarkService isolatedBenchmarkService) {
        this.benchmarkService = benchmarkService;
        this.isolatedBenchmarkService = isolatedBenchmarkService;
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
    
    /**
     * Endpoint za pokretanje izolovanog benchmark testiranja.
     * Izvršava kompletno testiranje serijalizacije i deserijalizacije JSON vs Protobuf
     * BEZ I/O overhead-a (RabbitMQ, mreža, disk).
     * 
     * Test generira 50 različitih poruka i za svaku izvršava 100 serijalizacija i 100 deserijalizacija.
     */
    @PostMapping("/isolated-benchmark")
    public DetailedBenchmarkResponse runIsolatedBenchmark() {
        IsolatedBenchmarkService.DetailedBenchmarkResult result = isolatedBenchmarkService.runIsolatedBenchmark();
        
        // Kreiraj DetailedBenchmarkResult objekte za JSON i Protobuf
        DetailedBenchmarkResult jsonResult = new DetailedBenchmarkResult(
            result.jsonSerializeNs,
            result.jsonDeserializeNs,
            result.jsonSizeBytes
        );
        
        DetailedBenchmarkResult protobufResult = new DetailedBenchmarkResult(
            result.protobufSerializeNs,
            result.protobufDeserializeNs,
            result.protobufSizeBytes
        );
        
        // Izračunaj statistiku poređenja
        DetailedBenchmarkResponse.DetailedComparisonStats comparison = calculateDetailedComparison(jsonResult, protobufResult);
        
        return new DetailedBenchmarkResponse(jsonResult, protobufResult, comparison);
    }
    
    private DetailedBenchmarkResponse.DetailedComparisonStats calculateDetailedComparison(
            DetailedBenchmarkResult json, DetailedBenchmarkResult protobuf) {
        
        if (json.getAvgSerializeNs() == 0 || protobuf.getAvgSerializeNs() == 0 ||
            json.getAvgDeserializeNs() == 0 || protobuf.getAvgDeserializeNs() == 0) {
            return new DetailedBenchmarkResponse.DetailedComparisonStats(0, 0, 0, 0, 0, 0);
        }
        
        // Speedup ratio (koliko puta je JSON sporiji od Protobuf-a)
        double serializeSpeedRatio = Math.round((json.getAvgSerializeNs() / (double) protobuf.getAvgSerializeNs()) * 100.0) / 100.0;
        double deserializeSpeedRatio = Math.round((json.getAvgDeserializeNs() / (double) protobuf.getAvgDeserializeNs()) * 100.0) / 100.0;
        double sizeRatio = Math.round((json.getAvgSizeBytes() / protobuf.getAvgSizeBytes()) * 100.0) / 100.0;
        
        // Procenat poboljšanja
        double serializeSpeedupPercent = Math.round((((json.getAvgSerializeNs() - protobuf.getAvgSerializeNs()) / (double) json.getAvgSerializeNs()) * 100.0) * 10.0) / 10.0;
        double deserializeSpeedupPercent = Math.round((((json.getAvgDeserializeNs() - protobuf.getAvgDeserializeNs()) / (double) json.getAvgDeserializeNs()) * 100.0) * 10.0) / 10.0;
        double compressionPercent = Math.round((((json.getAvgSizeBytes() - protobuf.getAvgSizeBytes()) / json.getAvgSizeBytes()) * 100.0) * 10.0) / 10.0;
        
        return new DetailedBenchmarkResponse.DetailedComparisonStats(
            serializeSpeedRatio, deserializeSpeedRatio, sizeRatio,
            serializeSpeedupPercent, deserializeSpeedupPercent, compressionPercent
        );
    }
}
