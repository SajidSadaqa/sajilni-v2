package com.sajilni.controller;

import com.sajilni.domain.GeneralResponse;
import com.sajilni.domain.request.RegisterReq;
import com.sajilni.domain.response.RegisterRes;
import com.sajilni.dto.*;
import com.sajilni.service.AuthFacade;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;

@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthFacade authFacade;

    public AuthController(AuthFacade authFacade) {
        this.authFacade = authFacade;
    }

    //TODO :  register, request,
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid RegisterReq registerReq, Locale locale) {
        return authFacade.register(registerReq, locale);
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody @Valid VerifyOtpDto dto, Locale locale) {
        return authFacade.verifyOtp(dto, locale);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginDto dto, Locale locale) {
        return authFacade.login(dto, locale);
    }
}