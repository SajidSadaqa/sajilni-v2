package com.sajilni.service;

import com.sajilni.dto.*;
import com.sajilni.entity.UserEntity;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class AuthFacade {
    private final UserService userService;
    private final OtpService otpService;
    private final MailService mailService;
    private final AuthenticationService authenticationService;
    private final MessageSource messages;

    public AuthFacade(UserService userService, OtpService otpService, MailService mailService,
                      AuthenticationService authenticationService, MessageSource messages) {
        this.userService = userService;
        this.otpService = otpService;
        this.mailService = mailService;
        this.authenticationService = authenticationService;
        this.messages = messages;
    }

    public ResponseEntity<String> register(RegisterDto dto, Locale locale) {
        UserEntity userEntity = userService.createUser(dto);
        String code = otpService.genrate(userEntity.getEmail());
        mailService.sendOtp(userEntity.getEmail(), code);
        String msg = messages.getMessage("user.registered", null, locale);
        return ResponseEntity.status(201).body(msg);
    }

    public ResponseEntity<String> verifyOtp(VerifyOtpDto dto, Locale locale) {
        boolean ok = otpService.verify(dto.getEmail(), dto.getOtp());
        if (!ok) {
            String msg = messages.getMessage("otp.invalid", null, locale);
            return ResponseEntity.badRequest().body(msg);
        }
        userService.enableUser(dto.getEmail());
        String msg = messages.getMessage("otp.verified", null, locale);
        return ResponseEntity.ok(msg);
    }

    public ResponseEntity<?> login(LoginDto dto, Locale locale) {
        return authenticationService.authenticate(dto, locale);
    }
}