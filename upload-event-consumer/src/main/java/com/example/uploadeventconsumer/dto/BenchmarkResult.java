package com.example.uploadeventconsumer.dto;

public class BenchmarkResult {
    private long avgDeserializeNs;
    private double avgSizeBytes;

    public BenchmarkResult() {
    }

    public BenchmarkResult(long avgDeserializeNs, double avgSizeBytes) {
        this.avgDeserializeNs = avgDeserializeNs;
        this.avgSizeBytes = avgSizeBytes;
    }

    public long getAvgDeserializeNs() {
        return avgDeserializeNs;
    }

    public void setAvgDeserializeNs(long avgDeserializeNs) {
        this.avgDeserializeNs = avgDeserializeNs;
    }

    public double getAvgSizeBytes() {
        return avgSizeBytes;
    }

    public void setAvgSizeBytes(double avgSizeBytes) {
        this.avgSizeBytes = avgSizeBytes;
    }
}
