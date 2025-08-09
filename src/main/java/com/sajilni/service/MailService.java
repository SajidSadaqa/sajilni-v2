package com.sajilni.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {
    private static final Logger log = LoggerFactory.getLogger(MailService.class);
    private final JavaMailSender mail;

    public MailService(JavaMailSender mail) {
        this.mail = mail;
    }

    public void sendOtp(String to, String otp) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setSubject("Your OTP Code");
        msg.setText("Your verification code is: " + otp + ". It expires in 30 minutes.");
        try {
            mail.send(msg);
        } catch (Exception ex) {
            log.error("Failed to send OTP email to {}", to, ex);
            throw new RuntimeException("email_send_failed");
        }
    }
}
