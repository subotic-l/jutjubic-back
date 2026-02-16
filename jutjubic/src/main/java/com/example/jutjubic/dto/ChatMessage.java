package com.example.jutjubic.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for real-time chat messages during live streaming.
 *
 * This message is ephemeral and not persisted to database.
 * Messages are only visible to users connected to the stream's chat topic.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    /**
     * Username of the message sender.
     * Can be extracted from JWT Principal for authenticated users.
     */
    private String sender;

    /**
     * The actual message content.
     */
    private String content;

    /**
     * The stream ID this message belongs to.
     * Used for routing to correct topic: /topic/stream/{streamId}
     */
    private Long streamId;

    /**
     * Timestamp when message was sent.
     * Set by server to ensure consistency.
     */
    private LocalDateTime timestamp;
}
