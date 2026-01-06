package com.example.jutjubic.controller;

import com.example.jutjubic.dto.JwtAuthenticationResponse;
import com.example.jutjubic.dto.LoginRequest;
import com.example.jutjubic.dto.RegisterRequest;
import com.example.jutjubic.dto.UserDto;
import com.example.jutjubic.model.User;
import com.example.jutjubic.security.TokenUtils;
import com.example.jutjubic.service.LoginAttemptService;
import com.example.jutjubic.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserService userService;

    @Autowired
    private TokenUtils tokenUtils;

    @Autowired
    private LoginAttemptService loginAttemptService;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        UserDto user = userService.registerUser(registerRequest);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Registration successful! Please check your email to activate your account.");
        response.put("user", user);
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@Valid @RequestBody LoginRequest loginRequest, 
                                      HttpServletRequest request) {
        String ipAddress = getClientIp(request);

        if (loginAttemptService.isBlocked(ipAddress)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Too many failed login attempts. Please try again in a minute."));
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            User user = (User) authentication.getPrincipal();
            String token = tokenUtils.generateToken(user.getEmail());

            loginAttemptService.recordLoginAttempt(ipAddress, true);

            JwtAuthenticationResponse response = new JwtAuthenticationResponse(token, tokenUtils.getExpiration());
            
            return ResponseEntity.ok(response);

        } catch (DisabledException e) {
            loginAttemptService.recordLoginAttempt(ipAddress, false);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Account is not activated. Please check your email for the activation link."));
        } catch (Exception e) {
            loginAttemptService.recordLoginAttempt(ipAddress, false);
            int remainingAttempts = loginAttemptService.getRemainingAttempts(ipAddress);
            
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "error", "Invalid credentials",
                            "remainingAttempts", remainingAttempts
                    ));
        }
    }

    @GetMapping("/verify")
    public ResponseEntity<String> verifyAccount(@RequestParam String token) {
        try {
            userService.verifyAccount(token);
            String html = "<!DOCTYPE html>" +
                    "<html lang='en'>" +
                    "<head><meta charset='UTF-8'><title>Account Verified</title></head>" +
                    "<body style='font-family: Arial, sans-serif; text-align: center;'>" +
                    "<h1>Account Verified!</h1>" +
                    "<p>Your account has been successfully activated.</p>" +
                    "</body></html>";
            return ResponseEntity.ok().contentType(org.springframework.http.MediaType.TEXT_HTML).body(html);
        } catch (Exception e) {
            String errorHtml = "<!DOCTYPE html>" +
                    "<html lang='en'>" +
                    "<head><meta charset='UTF-8'><title>Verification Failed</title></head>" +
                    "<body style='font-family: Arial, sans-serif; text-align: center;'>" +
                    "<h1>Verification Failed</h1>" +
                    "<p>" + e.getMessage() + "</p>" +
                    "</body></html>";
            return ResponseEntity.badRequest().contentType(org.springframework.http.MediaType.TEXT_HTML).body(errorHtml);
        }
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerification(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        userService.resendVerificationEmail(email);
        return ResponseEntity.ok(Map.of("message", "Verification email has been resent. Please check your inbox."));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();
        UserDto userDto = userService.convertToDto(user);
        return ResponseEntity.ok(userDto);
    }

    // Helper method to get client IP address
    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}
