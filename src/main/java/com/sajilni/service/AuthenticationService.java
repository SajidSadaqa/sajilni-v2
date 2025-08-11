package com.sajilni.service;

import com.sajilni.dto.LoginDto;
import com.sajilni.dto.TokenResponse;
import com.sajilni.entity.UserEntity;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class AuthenticationService {
    private final AuthenticationManager authManager;
    private final UserService userService;
    private final JwtService jwtService;
    private final MessageSource messages;

    public AuthenticationService(AuthenticationManager authManager, UserService userService,
                                 JwtService jwtService, MessageSource messages) {
        this.authManager = authManager;
        this.userService = userService;
        this.jwtService = jwtService;
        this.messages = messages;
    }

    public ResponseEntity<?> authenticate(LoginDto dto, Locale locale) {
        try {
            Authentication auth = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(dto.getEmail(), dto.getPassword()));

            if (!auth.isAuthenticated()) {
                throw new BadCredentialsException("Authentication failed");
            }

            UserEntity userEntity = userService.findByEmailOrThrow(dto.getEmail());
            String token = jwtService.generateToken(userEntity.getEmail(), userEntity.getId());

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