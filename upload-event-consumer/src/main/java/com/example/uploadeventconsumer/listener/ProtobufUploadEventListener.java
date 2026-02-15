package com.example.uploadeventconsumer.listener;

import com.example.uploadeventconsumer.service.BenchmarkService;
import com.jutjubic.common.proto.UploadEventProto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ProtobufUploadEventListener {

    private static final Logger logger = LoggerFactory.getLogger(ProtobufUploadEventListener.class);
    
    private final BenchmarkService benchmarkService;

    public ProtobufUploadEventListener(BenchmarkService benchmarkService) {
        this.benchmarkService = benchmarkService;
    }

    @RabbitListener(queues = "upload.proto")
    public void handleProtobufMessage(byte[] message) {
        try {
            int messageSize = message.length;
            
            // Measure deserialization time
            long startTime = System.nanoTime();
            UploadEventProto.UploadEvent event = UploadEventProto.UploadEvent.parseFrom(message);
            long endTime = System.nanoTime();
            
            long deserializeTime = endTime - startTime;
            
            // Record metrics
            benchmarkService.recordProtobufMetrics(deserializeTime, messageSize);
            
            logger.debug("Received Protobuf upload event: title={}, author={}, size={}, duration={}, uploadedAt={}, deserializeTime={}ns, messageSize={}bytes",
                    event.getTitle(), event.getAuthor(), event.getSize(), event.getDuration(), 
                    event.getUploadedAt(), deserializeTime, messageSize);
            
        } catch (Exception e) {
            logger.error("Error processing Protobuf message", e);
        }
    }
}
