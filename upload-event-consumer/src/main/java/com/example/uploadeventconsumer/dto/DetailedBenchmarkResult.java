package com.example.uploadeventconsumer.dto;

/**
 * Detaljni rezultat benchmark testiranja sa metrikama za serijalizaciju i deserijalizaciju.
 */
public class DetailedBenchmarkResult {
    private long avgSerializeNs;
    private long avgDeserializeNs;
    private double avgSizeBytes;

    public DetailedBenchmarkResult() {
    }

    public DetailedBenchmarkResult(long avgSerializeNs, long avgDeserializeNs, double avgSizeBytes) {
        this.avgSerializeNs = avgSerializeNs;
        this.avgDeserializeNs = avgDeserializeNs;
        this.avgSizeBytes = avgSizeBytes;
    }

    public long getAvgSerializeNs() {
        return avgSerializeNs;
    }

    public void setAvgSerializeNs(long avgSerializeNs) {
        this.avgSerializeNs = avgSerializeNs;
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
