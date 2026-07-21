package com._6.extractly.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailVerificationService {

    private final JavaMailSender mailSender;

    @Value("${app.baseUrl}")
    private String baseUrl;

    @Value("${spring.mail.username}")
    private String fromAddress;

    public EmailVerificationService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendVerificationEmail(String toEmail, String token) {
        String link = baseUrl + "/verify?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject("Verify your Extractly account");
        message.setText(
            "Welcome to Extractly!\n\n" +
            "Please verify your email by clicking the link below:\n" +
            link + "\n\n" +
            "This link does not expire, but you won't have full access until you verify.\n\n" +
            "If you didn't sign up for this, you can ignore this email."
        );

        mailSender.send(message);
    }
}