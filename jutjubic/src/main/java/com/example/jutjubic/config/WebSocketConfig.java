package com.example.jutjubic.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket configuration for real-time chat during live streaming.
 *
 * This configuration enables WebSocket + STOMP protocol for ephemeral chat.
 * Each live stream has its own chat room topic.
 *
 * Future scaling considerations:
 * - For multi-instance deployment, replace in-memory broker with external message broker (RabbitMQ/ActiveMQ)
 * - For message persistence and history, integrate Redis with pub/sub
 * - For user presence tracking, implement session-level tracking with Redis
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable simple in-memory message broker for broadcasting messages
        // Clients subscribe to topics like: /topic/stream/{streamId}
        config.enableSimpleBroker("/topic");

        // Messages from clients sent to @MessageMapping endpoints will have this prefix
        // Example: client sends to /app/chat.send/123 -> routes to @MessageMapping("/chat.send/{streamId}")
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket endpoint with SockJS fallback for browser compatibility
        // Clients connect to: ws://host/ws-chat or http://host/ws-chat (SockJS)
        // NO AUTHENTICATION - public access for live chat
        registry.addEndpoint("/ws-chat")
                .setAllowedOriginPatterns("*") // Configure allowed origins based on your frontend deployment
                .withSockJS(); // Enable SockJS fallback for browsers that don't support WebSocket
    }
}
