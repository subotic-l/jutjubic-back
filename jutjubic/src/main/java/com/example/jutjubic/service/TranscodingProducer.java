package com.example.jutjubic.service;

import com.example.jutjubic.config.RabbitMQConfig;
import com.example.jutjubic.dto.TranscodingMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TranscodingProducer {

    private final RabbitTemplate rabbitTemplate;

    public void sendTranscodingTask(TranscodingMessage message) {
        log.info("Sending transcoding task to queue for video ID: {}", message.getVideoId());
        rabbitTemplate.convertAndSend(RabbitMQConfig.TRANSCODING_QUEUE, message.toMessageString());
        log.info("Transcoding task sent successfully for video ID: {}", message.getVideoId());
    }
}