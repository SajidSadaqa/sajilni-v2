package com.sajilni.service;

import com.sajilni.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MailService {
    private final JavaMailSender mail;

    @Value("${EMAIL_USERNAME}")
    private String fromEmail;

    public MailService(JavaMailSender mail) {
        this.mail = mail;
    }

    public void sendOtp(String to, String otp) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(fromEmail);
        msg.setTo(to);
        msg.setSubject("Your OTP Code");
        msg.setText("Your verification code is: " + otp + ". It expires in 30 minutes.");

        try {
            mail.send(msg);
            log.info("OTP email sent successfully to {}", to);
        } catch (Exception ex) {
            log.error("Failed to send OTP email to {}", to, ex);
            throw new BusinessException("email.send.failed", "Failed to send email", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}