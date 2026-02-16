package com.example.uploadeventconsumer.service;

import com.example.uploadeventconsumer.dto.BenchmarkResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class BenchmarkService {

    private final List<Long> jsonDeserializeTimes = new ArrayList<>();
    private final List<Integer> jsonMessageSizes = new ArrayList<>();
    
    private final List<Long> protobufDeserializeTimes = new ArrayList<>();
    private final List<Integer> protobufMessageSizes = new ArrayList<>();

    public synchronized void recordJsonMetrics(long deserializeTimeNs, int messageSize) {
        jsonDeserializeTimes.add(deserializeTimeNs);
        jsonMessageSizes.add(messageSize);
    }

    public synchronized void recordProtobufMetrics(long deserializeTimeNs, int messageSize) {
        protobufDeserializeTimes.add(deserializeTimeNs);
        protobufMessageSizes.add(messageSize);
    }

    public synchronized BenchmarkResult getJsonBenchmark() {
        if (jsonDeserializeTimes.isEmpty()) {
            return new BenchmarkResult(0, 0.0);
        }
        
        long avgDeserializeNs = (long) jsonDeserializeTimes.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);
        
        double avgSizeBytes = jsonMessageSizes.stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);
        
        return new BenchmarkResult(avgDeserializeNs, avgSizeBytes);
    }

    public synchronized BenchmarkResult getProtobufBenchmark() {
        if (protobufDeserializeTimes.isEmpty()) {
            return new BenchmarkResult(0, 0.0);
        }
        
        long avgDeserializeNs = (long) protobufDeserializeTimes.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);
        
        double avgSizeBytes = protobufMessageSizes.stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);
        
        return new BenchmarkResult(avgDeserializeNs, avgSizeBytes);
    }

    public synchronized void reset() {
        jsonDeserializeTimes.clear();
        jsonMessageSizes.clear();
        protobufDeserializeTimes.clear();
        protobufMessageSizes.clear();
    }
}
