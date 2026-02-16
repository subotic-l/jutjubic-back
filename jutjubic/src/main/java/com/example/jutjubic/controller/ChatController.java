package com.example.jutjubic.controller;

import com.example.jutjubic.dto.ChatMessage;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime;

/**
 * WebSocket controller for real-time chat during live streaming.
 *
 * Handles chat messages and broadcasts them to subscribers of the stream's topic.
 * Messages are ephemeral (not persisted) and only visible from join time.
 *
 * Message Flow:
 * 1. Client sends message to: /app/chat.send/{streamId}
 * 2. Server validates and enriches message
 * 3. Server broadcasts to: /topic/stream/{streamId}
 * 4. All connected clients subscribed to that topic receive the message
 */
@Controller
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;

    public ChatController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Handles incoming chat messages from clients.
     *
     * @param streamId The ID of the live stream
     * @param chatMessage The message payload from client
     * @param headerAccessor STOMP headers for session info
     * @param principal Optional authenticated user principal (from JWT)
     */
    @MessageMapping("/chat.send/{streamId}")
    public void sendMessage(
            @DestinationVariable Long streamId,
            @Payload ChatMessage chatMessage,
            SimpMessageHeaderAccessor headerAccessor,
            Principal principal) {

        // Validate: ignore empty messages
        if (chatMessage.getContent() == null || chatMessage.getContent().trim().isEmpty()) {
            return; // Silently ignore empty messages
        }

        // Extract sender from authenticated principal if available
        // Falls back to provided sender name if principal is null
        String sender = (principal != null) ? principal.getName() : chatMessage.getSender();

        // Enrich message with server-side data
        ChatMessage enrichedMessage = ChatMessage.builder()
                .sender(sender)
                .content(chatMessage.getContent().trim())
                .streamId(streamId)
                .timestamp(LocalDateTime.now()) // Server timestamp ensures consistency
                .build();

        // Broadcast message to all subscribers of this stream's topic
        // Topic format: /topic/stream/{streamId}
        messagingTemplate.convertAndSend(
                "/topic/stream/" + streamId,
                enrichedMessage
        );
    }

    /**
     * Optional: Handle user join events
     * Uncomment if you want to notify when users join the chat
     */
    /*
    @MessageMapping("/chat.join/{streamId}")
    public void joinChat(
            @DestinationVariable Long streamId,
            @Payload ChatMessage chatMessage,
            Principal principal) {

        String username = (principal != null) ? principal.getName() : chatMessage.getSender();

        ChatMessage joinMessage = ChatMessage.builder()
                .sender("System")
                .content(username + " joined the chat")
                .streamId(streamId)
                .timestamp(LocalDateTime.now())
                .build();

        messagingTemplate.convertAndSend("/topic/stream/" + streamId, joinMessage);
    }
    */

    /**
     * Optional: Handle user leave events
     * Uncomment if you want to notify when users leave the chat
     */
    /*
    @MessageMapping("/chat.leave/{streamId}")
    public void leaveChat(
            @DestinationVariable Long streamId,
            @Payload ChatMessage chatMessage,
            Principal principal) {

        String username = (principal != null) ? principal.getName() : chatMessage.getSender();

        ChatMessage leaveMessage = ChatMessage.builder()
                .sender("System")
                .content(username + " left the chat")
                .streamId(streamId)
                .timestamp(LocalDateTime.now())
                .build();

        messagingTemplate.convertAndSend("/topic/stream/" + streamId, leaveMessage);
    }
    */
}
