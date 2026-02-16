package com.example.uploadeventconsumer.dto;

public class BenchmarkResponse {
    private BenchmarkResult json;
    private BenchmarkResult protobuf;
    private ComparisonStats comparison;

    public BenchmarkResponse() {
    }

    public BenchmarkResponse(BenchmarkResult json, BenchmarkResult protobuf, ComparisonStats comparison) {
        this.json = json;
        this.protobuf = protobuf;
        this.comparison = comparison;
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

    public ComparisonStats getComparison() {
        return comparison;
    }

    public void setComparison(ComparisonStats comparison) {
        this.comparison = comparison;
    }

    public static class ComparisonStats {
        private double speedRatio;
        private double sizeRatio;
        private double speedupPercent;
        private double compressionPercent;

        public ComparisonStats() {
        }

        public ComparisonStats(double speedRatio, double sizeRatio, double speedupPercent, double compressionPercent) {
            this.speedRatio = speedRatio;
            this.sizeRatio = sizeRatio;
            this.speedupPercent = speedupPercent;
            this.compressionPercent = compressionPercent;
        }

        public double getSpeedRatio() {
            return speedRatio;
        }

        public void setSpeedRatio(double speedRatio) {
            this.speedRatio = speedRatio;
        }

        public double getSizeRatio() {
            return sizeRatio;
        }

        public void setSizeRatio(double sizeRatio) {
            this.sizeRatio = sizeRatio;
        }

        public double getSpeedupPercent() {
            return speedupPercent;
        }

        public void setSpeedupPercent(double speedupPercent) {
            this.speedupPercent = speedupPercent;
        }

        public double getCompressionPercent() {
            return compressionPercent;
        }

        public void setCompressionPercent(double compressionPercent) {
            this.compressionPercent = compressionPercent;
        }
    }
}
