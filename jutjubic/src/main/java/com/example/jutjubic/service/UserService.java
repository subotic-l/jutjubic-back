package com.example.jutjubic.service;

import com.example.jutjubic.dto.RegisterRequest;
import com.example.jutjubic.dto.UserDto;
import com.example.jutjubic.model.User;
import com.example.jutjubic.model.VerificationToken;
import com.example.jutjubic.repository.UserRepository;
import com.example.jutjubic.repository.VerificationTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VerificationTokenRepository verificationTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    @Transactional
    public UserDto registerUser(RegisterRequest request) {
        User userByEmail = userRepository.findByEmail(request.getEmail()).orElse(null);
        if (userByEmail != null) {
            if (!userByEmail.isEnabled()) {
                VerificationToken token = verificationTokenRepository.findByUser(userByEmail).orElse(null);
                if (token != null && token.isExpired()) {
                    verificationTokenRepository.delete(token);
                    userRepository.delete(userByEmail);
                } else {
                    throw new RuntimeException("An account with this email already exists and is pending verification. Please check your email or request a new verification link.");
                }
            } else {
                throw new RuntimeException("Email is already registered");
            }
        }

        User userByUsername = userRepository.findByUsername(request.getUsername()).orElse(null);
        if (userByUsername != null) {
            if (!userByUsername.isEnabled()) {
                VerificationToken token = verificationTokenRepository.findByUser(userByUsername).orElse(null);
                if (token != null && token.isExpired()) {
                    verificationTokenRepository.delete(token);
                    userRepository.delete(userByUsername);
                } else {
                    throw new RuntimeException("This username is taken by an unverified account. Please choose a different username or wait for the previous registration to expire.");
                }
            } else {
                throw new RuntimeException("Username is already taken");
            }
        }

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Passwords do not match");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setAddress(request.getAddress());
        user.setEnabled(false);

        User savedUser = userRepository.save(user);

        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = new VerificationToken(token, savedUser);
        verificationTokenRepository.save(verificationToken);

        emailService.sendVerificationEmail(savedUser.getEmail(), token);

        return convertToDto(savedUser);
    }

    @Transactional
    public void verifyAccount(String token) {
        VerificationToken verificationToken = verificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid token"));

        if (verificationToken.isUsed()) {
            throw new RuntimeException("Token has already been used");
        }

        if (verificationToken.isExpired()) {
            throw new RuntimeException("Token has expired");
        }

        User user = verificationToken.getUser();
        user.setEnabled(true);
        userRepository.save(user);

        verificationToken.setUsed(true);
        verificationTokenRepository.save(verificationToken);
    }

    @Transactional
    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.isEnabled()) {
            throw new RuntimeException("Account is already verified");
        }

        VerificationToken oldToken = verificationTokenRepository.findByUser(user).orElse(null);
        if (oldToken != null) {
            verificationTokenRepository.delete(oldToken);
            verificationTokenRepository.flush();
        }

        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = new VerificationToken(token, user);
        verificationTokenRepository.save(verificationToken);

        emailService.sendVerificationEmail(user.getEmail(), token);
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public UserDto convertToDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setUsername(user.getUsername());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setAddress(user.getAddress());
        return dto;
    }
}
