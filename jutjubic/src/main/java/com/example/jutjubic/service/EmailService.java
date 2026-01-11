package com.example.jutjubic.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.backend.url}")
    private String backendUrl;

    public void sendVerificationEmail(String toEmail, String token) {
        String subject = "Account Activation - Jutjubic";
        String verificationUrl = backendUrl + "/api/auth/verify?token=" + token;
        
        String message = "Welcome!\n\n" +
                "Please activate your account by clicking the following link:\n" +
                verificationUrl + "\n\n" +
                "This link is valid for 24 hours.\n\n" +
                "If you did not create this account, please ignore this email.\n\n" +
                "Best regards,\nJutjubic Team";

        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setFrom(fromEmail);
        mailMessage.setTo(toEmail);
        mailMessage.setSubject(subject);
        mailMessage.setText(message);

        mailSender.send(mailMessage);
    }
}
