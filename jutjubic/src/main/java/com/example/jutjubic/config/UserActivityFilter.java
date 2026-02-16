package com.example.jutjubic.config;

import com.example.jutjubic.model.User;
import com.example.jutjubic.service.ActiveUserMetricsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class UserActivityFilter extends OncePerRequestFilter {

    @Autowired
    private ActiveUserMetricsService activeUserMetricsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        // Track activity samo za autentifikovane korisnike
        if (authentication != null && authentication.isAuthenticated() 
            && authentication.getPrincipal() instanceof User) {
            User user = (User) authentication.getPrincipal();
            activeUserMetricsService.recordUserActivity(user.getEmail());
        }
        
        filterChain.doFilter(request, response);
    }
}
