package com.example.jutjubic.service;

import com.example.jutjubic.config.RabbitMQConfig;
import com.example.jutjubic.dto.TranscodingMessage;
import com.example.jutjubic.model.VideoPost;
import com.example.jutjubic.repository.VideoPostRepository;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TranscodingConsumer {

    private final FFmpegService ffmpegService;
    private final VideoPostRepository videoPostRepository;

    @RabbitListener(queues = RabbitMQConfig.TRANSCODING_QUEUE, containerFactory = "rabbitListenerContainerFactory")
    public void processTranscodingTask(String messageString, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        TranscodingMessage message = null;

        try {
            message = TranscodingMessage.fromMessageString(messageString);
            String consumerName = Thread.currentThread().getName();

            log.info("[{}] Received transcoding task for video ID: {}", consumerName, message.getVideoId());
            log.info("[{}] Input: {}, Output: {}", consumerName, message.getOriginalVideoPath(), message.getOutputVideoPath());

            boolean success = ffmpegService.transcodeVideo(
                    message.getOriginalVideoPath(),
                    message.getOutputVideoPath(),
                    message.getResolution(),
                    message.getBitrate(),
                    message.getCodec()
            );

            if (success) {
                updateVideoPostWithTranscodedPath(message.getVideoId(), message.getOutputVideoPath());
                log.info("[{}] Transcoding completed successfully for video ID: {}", consumerName, message.getVideoId());

                channel.basicAck(deliveryTag, false);
            } else {
                log.error("[{}] Transcoding failed for video ID: {}", consumerName, message.getVideoId());

                channel.basicReject(deliveryTag, false);
            }

        } catch (Exception e) {
            log.error("Error processing transcoding task: {}", e.getMessage(), e);

            try {
                channel.basicReject(deliveryTag, false);
            } catch (IOException ioException) {
                log.error("Error rejecting message: {}", ioException.getMessage());
            }
        }
    }

    private void updateVideoPostWithTranscodedPath(Long videoId, String transcodedPath) {
        Optional<VideoPost> videoPostOpt = videoPostRepository.findById(videoId);

        if (videoPostOpt.isPresent()) {
            VideoPost videoPost = videoPostOpt.get();
            videoPost.setTranscodedVideoUrl(transcodedPath);
            videoPostRepository.save(videoPost);
            log.info("Updated VideoPost {} with transcoded path: {}", videoId, transcodedPath);
        } else {
            log.warn("VideoPost with ID {} not found, cannot update transcoded path", videoId);
        }
    }
}