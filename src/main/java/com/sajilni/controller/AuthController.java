package com.sajilni.controller;

import com.sajilni.dto.*;
import com.sajilni.entity.User;
import com.sajilni.service.*;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;



import java.util.Locale;

@RestController
@RequestMapping("/api/auth")

public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final UserService userService;
    private final OtpService otpService;
    private final MailService mailService;
    private final JwtService jwtService;
    private final AuthenticationManager authManager;
    private final MessageSource messages;

    public AuthController(UserService userService, OtpService otpService, MailService mailService, JwtService jwtService, AuthenticationManager authManager, MessageSource messages) {
        this.userService = userService;
        this.otpService = otpService;
        this.mailService = mailService;
        this.jwtService = jwtService;
        this.authManager = authManager;
        this.messages = messages;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody @Valid RegisterDto dto, Locale locale) {
        User user = userService.createUser(dto);
        String code = otpService.generate(user.getEmail());
        mailService.sendOtp(user.getEmail(), code);
        String msg = messages.getMessage("user.registered", null, locale);
        return ResponseEntity.status(201).body(msg);
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<String> verifyOtp(@RequestBody @Valid VerifyOtpDto dto, Locale locale) {
        boolean ok = otpService.verify(dto.getEmail(), dto.getOtp());
        if (!ok) {
            String msg = messages.getMessage("otp.invalid", null, locale);
            return ResponseEntity.badRequest().body(msg);
        }
        userService.enableUser(dto.getEmail());
        String msg = messages.getMessage("otp.verified", null, locale);
        return ResponseEntity.ok(msg);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginDto dto, Locale locale) {
        try {
            Authentication auth = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(dto.getEmail(), dto.getPassword()));
            if (!auth.isAuthenticated()) {
                throw new BadCredentialsException("bad");
            }
            User user = userService.findByEmailOrThrow(dto.getEmail());
            String token = jwtService.generateToken(user.getEmail(), user.getId());
            return ResponseEntity.ok(new TokenResponse(token));
        } catch (DisabledException e) {
            String msg = messages.getMessage("auth.unverified", null, locale);
            return ResponseEntity.status(403).body(msg);
        } catch (AuthenticationException e) {
            String msg = messages.getMessage("auth.bad", null, locale);
            return ResponseEntity.status(401).body(msg);
        }
    }
}
