package com.example.uploadeventconsumer.dto;

public class BenchmarkResponse {
    private BenchmarkResult json;
    private BenchmarkResult protobuf;

    public BenchmarkResponse() {
    }

    public BenchmarkResponse(BenchmarkResult json, BenchmarkResult protobuf) {
        this.json = json;
        this.protobuf = protobuf;
    }

    public BenchmarkResult getJson() {
        return json;
    }

    public void setJson(BenchmarkResult json) {
        this.json = json;
    }

    public BenchmarkResult getProtobuf() {
        return protobuf;
    }

    public void setProtobuf(BenchmarkResult protobuf) {
        this.protobuf = protobuf;
    }
}
