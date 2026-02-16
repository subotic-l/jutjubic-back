package com.example.uploadeventconsumer.dto;

/**
 * Response sa detaljnim benchmark rezultatima uključujući serijalizaciju i deserijalizaciju.
 */
public class DetailedBenchmarkResponse {
    private DetailedBenchmarkResult json;
    private DetailedBenchmarkResult protobuf;
    private DetailedComparisonStats comparison;

    public DetailedBenchmarkResponse() {
    }

    public DetailedBenchmarkResponse(DetailedBenchmarkResult json, DetailedBenchmarkResult protobuf, DetailedComparisonStats comparison) {
        this.json = json;
        this.protobuf = protobuf;
        this.comparison = comparison;
    }

    public DetailedBenchmarkResult getJson() {
        return json;
    }

    public void setJson(DetailedBenchmarkResult json) {
        this.json = json;
    }

    public DetailedBenchmarkResult getProtobuf() {
        return protobuf;
    }

    public void setProtobuf(DetailedBenchmarkResult protobuf) {
        this.protobuf = protobuf;
    }

    public DetailedComparisonStats getComparison() {
        return comparison;
    }

    public void setComparison(DetailedComparisonStats comparison) {
        this.comparison = comparison;
    }

    /**
     * Statistika poređenja JSON vs Protobuf.
     */
    public static class DetailedComparisonStats {
        private double serializeSpeedRatio;
        private double deserializeSpeedRatio;
        private double sizeRatio;
        
        private double serializeSpeedupPercent;
        private double deserializeSpeedupPercent;
        private double compressionPercent;

        public DetailedComparisonStats() {
        }

        public DetailedComparisonStats(double serializeSpeedRatio, double deserializeSpeedRatio, double sizeRatio,
                                       double serializeSpeedupPercent, double deserializeSpeedupPercent, double compressionPercent) {
            this.serializeSpeedRatio = serializeSpeedRatio;
            this.deserializeSpeedRatio = deserializeSpeedRatio;
            this.sizeRatio = sizeRatio;
            this.serializeSpeedupPercent = serializeSpeedupPercent;
            this.deserializeSpeedupPercent = deserializeSpeedupPercent;
            this.compressionPercent = compressionPercent;
        }

        public double getSerializeSpeedRatio() {
            return serializeSpeedRatio;
        }

        public void setSerializeSpeedRatio(double serializeSpeedRatio) {
            this.serializeSpeedRatio = serializeSpeedRatio;
        }

        public double getDeserializeSpeedRatio() {
            return deserializeSpeedRatio;
        }

        public void setDeserializeSpeedRatio(double deserializeSpeedRatio) {
            this.deserializeSpeedRatio = deserializeSpeedRatio;
        }

        public double getSizeRatio() {
            return sizeRatio;
        }

        public void setSizeRatio(double sizeRatio) {
            this.sizeRatio = sizeRatio;
        }

        public double getSerializeSpeedupPercent() {
            return serializeSpeedupPercent;
        }

        public void setSerializeSpeedupPercent(double serializeSpeedupPercent) {
            this.serializeSpeedupPercent = serializeSpeedupPercent;
        }

        public double getDeserializeSpeedupPercent() {
            return deserializeSpeedupPercent;
        }

        public void setDeserializeSpeedupPercent(double deserializeSpeedupPercent) {
            this.deserializeSpeedupPercent = deserializeSpeedupPercent;
        }

        public double getCompressionPercent() {
            return compressionPercent;
        }

        public void setCompressionPercent(double compressionPercent) {
            this.compressionPercent = compressionPercent;
        }
    }
}
