package com.sajilni.service;

import com.sajilni.domain.request.RegisterReq;
import com.sajilni.dto.*;
import com.sajilni.entity.UserEntity;
import com.sajilni.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
public class AuthFacade {

    private final UserService userService;
    private final OtpService otpService;
    private final MailService mailService;
    private final AuthenticationService authenticationService;
    private final MessageSource messageSource;

    public AuthFacade(UserService userService,
                      OtpService otpService,
                      MailService mailService,
                      AuthenticationService authenticationService,
                      MessageSource messageSource) {
        this.userService = userService;
        this.otpService = otpService;
        this.mailService = mailService;
        this.authenticationService = authenticationService;
        this.messageSource = messageSource;
    }

    @Transactional
    public ResponseEntity<ApiResponse<RegisterResponse>> register(RegisterReq registerReq) {
        Locale locale = LocaleContextHolder.getLocale();

        try {
            // Create user (will throw exception if exists)
            UserEntity userEntity = userService.createUser(registerReq);

            // Generate OTP with challenge tracking
            String otpCode = otpService.generate(userEntity.getEmail());

            // Send OTP email asynchronously
            mailService.sendOtpAsync(userEntity.getEmail(), otpCode, userEntity.getFirstName());

            String message = messageSource.getMessage("user.registered", null, locale);

            RegisterResponse response = RegisterResponse.builder()
                    .email(userEntity.getEmail())
                    .remainingAttempts(otpService.getRemainingAttempts(userEntity.getEmail()))
                    .build();

            log.info("User registered successfully: {}", userEntity.getEmail());

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.<RegisterResponse>builder()
                            .success(true)
                            .message(message)
                            .data(response)
                            .build());

        } catch (Exception ex) {
            log.error("Registration failed for email: {}", registerReq.getEmail(), ex);
            throw ex; // Re-throw to be handled by global exception handler
        }
    }

    @Transactional
    public ResponseEntity<ApiResponse<OtpVerificationResponse>> verifyOtp(VerifyOtpDto dto) {
        Locale locale = LocaleContextHolder.getLocale();

        try {
            boolean isValid = otpService.verify(dto.getEmail(), dto.getOtp());

            if (!isValid) {
                int remainingAttempts = otpService.getRemainingAttempts(dto.getEmail());

                if (remainingAttempts <= 0) {
                    String message = messageSource.getMessage("otp.locked", null, locale);
                    throw new BusinessException("otp.locked", message, HttpStatus.FORBIDDEN);
                }

                String message = messageSource.getMessage("otp.invalid", null, locale);

                OtpVerificationResponse response = OtpVerificationResponse.builder()
                        .verified(false)
                        .remainingAttempts(remainingAttempts)
                        .build();

                return ResponseEntity.badRequest()
                        .body(ApiResponse.<OtpVerificationResponse>builder()
                                .success(false)
                                .message(message)
                                .data(response)
                                .build());
            }

            // Enable user and get details for welcome email
            UserEntity user = userService.enableUser(dto.getEmail());

            // Send welcome email asynchronously
            mailService.sendWelcomeEmailAsync(user.getEmail(), user.getFirstName());

            String message = messageSource.getMessage("otp.verified", null, locale);

            OtpVerificationResponse response = OtpVerificationResponse.builder()
                    .verified(true)
                    .email(user.getEmail())
                    .userId(user.getId())
                    .build();

            log.info("Email verified successfully: {}", user.getEmail());

            return ResponseEntity.ok(ApiResponse.<OtpVerificationResponse>builder()
                    .success(true)
                    .message(message)
                    .data(response)
                    .build());

        } catch (BusinessException ex) {
            throw ex; // Re-throw business exceptions
        } catch (Exception ex) {
            log.error("OTP verification failed for email: {}", dto.getEmail(), ex);
            throw new BusinessException("otp.verification.failed",
                    "OTP verification failed", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResponseEntity<ApiResponse<LoginResponse>> login(LoginDto dto) {
        try {
            return authenticationService.authenticate(dto);
        } catch (Exception ex) {
            log.error("Login failed for email: {}", dto.getEmail(), ex);
            throw ex; // Re-throw to be handled by global exception handler
        }
    }

    public ResponseEntity<ApiResponse<OtpResponse>> requestNewOtp(RequestOtpDto dto) {
        Locale locale = LocaleContextHolder.getLocale();

        try {
            // Check if user exists and is not verified
            UserEntity user = userService.findByEmailOrThrow(dto.getEmail());

            if (user.isEnabled()) {
                throw new BusinessException("user.already.verified",
                        "User is already verified", HttpStatus.BAD_REQUEST);
            }

            // Generate new OTP (will handle rate limiting internally)
            String otpCode = otpService.generate(user.getEmail());

            // Send OTP email
            mailService.sendOtpAsync(user.getEmail(), otpCode, user.getFirstName());

            String message = messageSource.getMessage("otp.sent", null,
                    "New verification code sent to your email", locale);

            OtpResponse response = OtpResponse.builder()
                    .email(user.getEmail())
                    .remainingAttempts(otpService.getRemainingAttempts(user.getEmail()))
                    .build();

            log.info("New OTP requested for: {}", user.getEmail());

            return ResponseEntity.ok(ApiResponse.<OtpResponse>builder()
                    .success(true)
                    .message(message)
                    .data(response)
                    .build());

        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to request new OTP for: {}", dto.getEmail(), ex);
            throw new BusinessException("otp.request.failed",
                    "Failed to send new OTP", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResponseEntity<ApiResponse<TokenResponse>> refreshToken(RefreshTokenDto dto) {
        try {
            return authenticationService.refreshToken(dto);
        } catch (Exception ex) {
            log.error("Token refresh failed", ex);
            throw ex;
        }
    }
}