package com.example.jutjubic.security;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

/**
 * WebSocket channel interceptor for authenticating STOMP messages.
 * 
 * This interceptor validates JWT tokens sent via STOMP CONNECT frames
 * and sets the authentication context for subsequent messages.
 */
@Component
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private final TokenUtils tokenUtils;
    private final UserDetailsService userDetailsService;

    public WebSocketAuthChannelInterceptor(TokenUtils tokenUtils, UserDetailsService userDetailsService) {
        this.tokenUtils = tokenUtils;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = null;
            
            // First, try to get token from STOMP headers (sent by client during CONNECT)
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }
            
            // If not in headers, try to get from WebSocket session attributes (set during handshake)
            if (token == null) {
                Object tokenAttr = accessor.getSessionAttributes().get("token");
                if (tokenAttr != null) {
                    token = tokenAttr.toString();
                    System.out.println("WebSocket CONNECT: Using token from session attributes");
                }
            }

            if (token != null) {
                try {
                    String username = tokenUtils.getUsernameFromToken(token);
                    
                    if (username != null) {
                        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                        
                        if (tokenUtils.validateToken(token, userDetails)) {
                            UsernamePasswordAuthenticationToken authentication = 
                                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                            
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                            accessor.setUser(authentication);
                            System.out.println("WebSocket CONNECT: User authenticated - " + username);
                        }
                    }
                } catch (Exception e) {
                    // Invalid token - connection will be rejected
                    System.err.println("WebSocket authentication failed: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                System.err.println("WebSocket CONNECT: No token found in headers or session attributes");
            }
        }

        return message;
    }
}
