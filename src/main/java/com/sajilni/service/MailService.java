package com.sajilni.service;

import com.sajilni.exception.BusinessException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class MailService {
    private final JavaMailSender mailSender;

    @Value("${EMAIL_USERNAME}")
    private String fromEmail;

    @Value("${app.name:Sajilni}")
    private String appName;

    @Value("${app.website:https://sajilni.com}")
    private String website;

    @Value("${app.support.email:support@sajilni.com}")
    private String supportEmail;

    public MailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOtp(String to, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, appName);
            helper.setTo(to);
            helper.setSubject("🔐 Your " + appName + " Verification Code");

            String htmlContent = buildOtpEmailTemplate(otp, to);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Professional OTP email sent successfully to {}", to);

        } catch (Exception ex) {
            log.error("Failed to send OTP email to {}", to, ex);
            throw new BusinessException("email.send.failed", "Failed to send email", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Async
    public CompletableFuture<Void> sendOtpAsync(String to, String otp, String firstName) {
        try {
            sendOtp(to, otp);
            return CompletableFuture.completedFuture(null);
        } catch (Exception ex) {
            log.error("Async OTP email sending failed for {}", to, ex);
            return CompletableFuture.failedFuture(ex);
        }
    }

    public void sendWelcomeEmail(String to, String firstName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, appName);
            helper.setTo(to);
            helper.setSubject("🎉 Welcome to " + appName + "!");

            String htmlContent = buildWelcomeEmailTemplate(firstName);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Welcome email sent successfully to {}", to);

        } catch (Exception ex) {
            log.error("Failed to send welcome email to {}", to, ex);
        }
    }

    @Async
    public CompletableFuture<Void> sendWelcomeEmailAsync(String to, String firstName) {
        try {
            sendWelcomeEmail(to, firstName);
            return CompletableFuture.completedFuture(null);
        } catch (Exception ex) {
            log.error("Async welcome email sending failed for {}", to, ex);
            return CompletableFuture.failedFuture(ex);
        }
    }

    private String buildOtpEmailTemplate(String otp, String email) {
        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a"));

        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Your Verification Code</title>
                <style>
                    * {
                        margin: 0;
                        padding: 0;
                        box-sizing: border-box;
                    }
                    
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
                        line-height: 1.6;
                        color: #333333;
                        background-color: #f8fafc;
                    }
                    
                    .container {
                        max-width: 600px;
                        margin: 0 auto;
                        background-color: #ffffff;
                        border-radius: 12px;
                        overflow: hidden;
                        box-shadow: 0 4px 25px rgba(0, 0, 0, 0.1);
                        margin-top: 40px;
                        margin-bottom: 40px;
                    }
                    
                    .header {
                        background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                        color: white;
                        padding: 40px 30px;
                        text-align: center;
                    }
                    
                    .header h1 {
                        font-size: 28px;
                        font-weight: 700;
                        margin-bottom: 8px;
                    }
                    
                    .header p {
                        font-size: 16px;
                        opacity: 0.9;
                    }
                    
                    .content {
                        padding: 40px 30px;
                        text-align: center;
                    }
                    
                    .greeting {
                        font-size: 18px;
                        color: #374151;
                        margin-bottom: 24px;
                    }
                    
                    .otp-container {
                        background: linear-gradient(135deg, #f3f4f6 0%%, #e5e7eb 100%%);
                        border-radius: 16px;
                        padding: 32px;
                        margin: 32px 0;
                        border: 2px dashed #d1d5db;
                    }
                    
                    .otp-label {
                        font-size: 14px;
                        color: #6b7280;
                        text-transform: uppercase;
                        letter-spacing: 1px;
                        font-weight: 600;
                        margin-bottom: 12px;
                    }
                    
                    .otp-code {
                        font-size: 36px;
                        font-weight: 800;
                        color: #1f2937;
                        letter-spacing: 8px;
                        font-family: 'Courier New', monospace;
                        background: white;
                        padding: 20px;
                        border-radius: 12px;
                        display: inline-block;
                        min-width: 280px;
                        box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
                        border: 1px solid #e5e7eb;
                    }
                    
                    .warning-box {
                        background-color: #fef3cd;
                        border: 1px solid #fbbf24;
                        border-radius: 8px;
                        padding: 16px;
                        margin: 24px 0;
                    }
                    
                    .warning-box p {
                        color: #92400e;
                        font-size: 14px;
                        margin: 0;
                    }
                    
                    .info-text {
                        color: #6b7280;
                        font-size: 14px;
                        line-height: 1.5;
                        margin: 20px 0;
                    }
                    
                    .cta-button {
                        display: inline-block;
                        background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                        color: white;
                        text-decoration: none;
                        padding: 14px 32px;
                        border-radius: 50px;
                        font-weight: 600;
                        font-size: 16px;
                        margin: 24px 0;
                        transition: transform 0.2s ease;
                    }
                    
                    .cta-button:hover {
                        transform: translateY(-2px);
                        box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
                    }
                    
                    .footer {
                        background-color: #f9fafb;
                        padding: 30px;
                        text-align: center;
                        border-top: 1px solid #e5e7eb;
                    }
                    
                    .footer-content {
                        color: #6b7280;
                        font-size: 12px;
                        line-height: 1.5;
                    }
                    
                    .social-links {
                        margin: 20px 0;
                    }
                    
                    .social-links a {
                        display: inline-block;
                        margin: 0 8px;
                        color: #9ca3af;
                        text-decoration: none;
                        font-size: 12px;
                    }
                    
                    .divider {
                        height: 1px;
                        background: linear-gradient(90deg, transparent, #e5e7eb, transparent);
                        margin: 30px 0;
                    }
                    
                    @media (max-width: 600px) {
                        .container {
                            margin: 20px;
                            border-radius: 8px;
                        }
                        
                        .content {
                            padding: 30px 20px;
                        }
                        
                        .otp-code {
                            font-size: 28px;
                            letter-spacing: 4px;
                            min-width: auto;
                        }
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🔐 %s</h1>
                        <p>Secure verification code</p>
                    </div>
                    
                    <div class="content">
                        <div class="greeting">
                            Hi there! 👋
                        </div>
                        
                        <p>We received a request to verify your email address. Use the verification code below to complete your registration:</p>
                        
                        <div class="otp-container">
                            <div class="otp-label">Your Verification Code</div>
                            <div class="otp-code">%s</div>
                        </div>
                        
                        <div class="warning-box">
                            <p>⚠️ This code expires in 30 minutes and can only be used once.</p>
                        </div>
                        
                        <div class="info-text">
                            If you didn't request this code, you can safely ignore this email. 
                            Your account security is important to us.
                        </div>
                        
                        <div class="divider"></div>
                        
                        <p class="info-text">
                            <strong>Need help?</strong> Contact our support team at 
                            <a href="mailto:%s" style="color: #667eea;">%s</a>
                        </p>
                    </div>
                    
                    <div class="footer">
                        <div class="footer-content">
                            <p><strong>%s Team</strong></p>
                            <p>This email was sent to %s on %s</p>
                            <p>© 2024 %s. All rights reserved.</p>
                            
                            <div class="social-links">
                                <a href="%s">Website</a>
                                <a href="%s/privacy">Privacy Policy</a>
                                <a href="%s/terms">Terms of Service</a>
                            </div>
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(
                appName, otp, supportEmail, supportEmail, appName,
                email, currentTime, appName, website, website, website
        );
    }

    private String buildWelcomeEmailTemplate(String firstName) {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Welcome to %s!</title>
                <style>
                    /* Similar CSS styles as above, but with welcome theme */
                    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; }
                    .container { max-width: 600px; margin: 0 auto; background: white; border-radius: 12px; }
                    .header { background: linear-gradient(135deg, #10b981 0%%, #059669 100%%); color: white; padding: 40px; text-align: center; }
                    .content { padding: 40px 30px; text-align: center; }
                    .welcome-message { font-size: 24px; color: #1f2937; margin: 20px 0; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🎉 Welcome to %s!</h1>
                        <p>Your journey begins here</p>
                    </div>
                    <div class="content">
                        <div class="welcome-message">
                            Hello %s! 🎈
                        </div>
                        <p>Thank you for joining our community. We're excited to have you on board!</p>
                        <p>Your email has been successfully verified and your account is now active.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(appName, appName, firstName);
    }
}