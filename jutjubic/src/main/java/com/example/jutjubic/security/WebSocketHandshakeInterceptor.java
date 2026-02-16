package com.example.jutjubic.security;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket handshake interceptor to extract JWT token from query parameters.
 * 
 * This interceptor runs BEFORE Spring Security filters during WebSocket handshake.
 * It extracts the token from query parameter and stores it in WebSocket session attributes
 * for later use by the channel interceptor.
 */
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, 
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler, 
                                   Map<String, Object> attributes) throws Exception {
        
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
            
            // Extract token from query parameter
            String token = servletRequest.getServletRequest().getParameter("token");
            
            if (token != null && !token.isEmpty()) {
                // Store token in WebSocket session attributes
                // This will be available in WebSocketAuthChannelInterceptor
                attributes.put("token", token);
                System.out.println("WebSocket handshake: Token extracted from query parameter");
            } else {
                System.out.println("WebSocket handshake: No token found in query parameter");
            }
        }
        
        return true; // Allow handshake to proceed
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, 
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler, 
                               Exception exception) {
        // Nothing to do after handshake
    }
}
