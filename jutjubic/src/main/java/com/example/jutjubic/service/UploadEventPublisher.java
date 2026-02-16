package com.example.jutjubic.service;

import com.example.jutjubic.dto.UploadEventDto;
import com.example.proto.UploadEventProto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
@Slf4j
public class UploadEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    
    private static final String JSON_QUEUE = "upload.json";
    private static final String PROTO_QUEUE = "upload.proto";

    /**
     * Šalje upload event u obe queue (JSON i Protobuf) za benchmark
     */
    public void publishUploadEvent(UploadEventDto event) {
        try {
            // Šalji JSON verziju
            publishJsonEvent(event);
            
            // Šalji Protobuf verziju
            publishProtobufEvent(event);
            
            log.debug("Published upload event for video {}: {} to both queues", 
                event.getVideoId(), event.getTitle());
        } catch (Exception e) {
            log.error("Failed to publish upload event for video {}", event.getVideoId(), e);
        }
    }

    private void publishJsonEvent(UploadEventDto event) {
        try {
            rabbitTemplate.convertAndSend(JSON_QUEUE, event);
            log.debug("Sent JSON message to {}", JSON_QUEUE);
        } catch (Exception e) {
            log.error("Failed to send JSON message to queue", e);
        }
    }

    private void publishProtobufEvent(UploadEventDto event) {
        try {
            // Konvertuj DTO u Protobuf
            UploadEventProto.UploadEvent.Builder builder = UploadEventProto.UploadEvent.newBuilder()
                .setVideoId(event.getVideoId())
                .setTitle(event.getTitle())
                .setUsername(event.getUsername())
                .setVideoUrl(event.getVideoUrl())
                .setUploadedAt(event.getUploadedAt().toEpochSecond(ZoneOffset.UTC));
            
            if (event.getLatitude() != null) {
                builder.setLatitude(event.getLatitude());
            }
            if (event.getLongitude() != null) {
                builder.setLongitude(event.getLongitude());
            }
            
            byte[] protoBytes = builder.build().toByteArray();
            
            // Šalji kao raw byte array bez Jackson konverzije
            MessageProperties props = new MessageProperties();
            props.setContentType("application/octet-stream");
            Message message = new Message(protoBytes, props);
            
            rabbitTemplate.send(PROTO_QUEUE, message);
            log.debug("Sent Protobuf message to {} ({} bytes)", PROTO_QUEUE, protoBytes.length);
        } catch (Exception e) {
            log.error("Failed to send Protobuf message to queue", e);
        }
    }
}
