package com.example.uploadeventconsumer.listener;

import com.example.uploadeventconsumer.dto.UploadEventJson;
import com.example.uploadeventconsumer.service.BenchmarkService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class JsonUploadEventListener {

    private static final Logger logger = LoggerFactory.getLogger(JsonUploadEventListener.class);
    
    private final BenchmarkService benchmarkService;
    private final ObjectMapper objectMapper;

    public JsonUploadEventListener(BenchmarkService benchmarkService, ObjectMapper objectMapper) {
        this.benchmarkService = benchmarkService;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = "upload.json")
    public void handleJsonMessage(byte[] message) {
        try {
            int messageSize = message.length;
            
            // Measure deserialization time
            long startTime = System.nanoTime();
            UploadEventJson event = objectMapper.readValue(message, UploadEventJson.class);
            long endTime = System.nanoTime();
            
            long deserializeTime = endTime - startTime;
            
            // Record metrics
            benchmarkService.recordJsonMetrics(deserializeTime, messageSize);
            
            logger.debug("Received JSON upload event: title={}, author={}, size={}, duration={}, uploadedAt={}, deserializeTime={}ns, messageSize={}bytes",
                    event.getTitle(), event.getAuthor(), event.getSize(), event.getDuration(), 
                    event.getUploadedAt(), deserializeTime, messageSize);
            
        } catch (Exception e) {
            logger.error("Error processing JSON message", e);
        }
    }
}
