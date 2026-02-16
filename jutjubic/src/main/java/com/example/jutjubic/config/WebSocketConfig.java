package com.example.jutjubic.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Prefix za poruke koje idu ka klijentima (subscribe)
        config.enableSimpleBroker("/topic");

        // Prefix za poruke koje dolaze od klijenata (send)
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket endpoint - klijenti se konektuju ovde
        registry.addEndpoint("/ws")
                .setAllowedOrigins("http://localhost:4200", "http://localhost:3000")
                .withSockJS();
    }
}