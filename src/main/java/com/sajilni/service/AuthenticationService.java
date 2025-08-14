package com.sajilni.service;

import com.sajilni.dto.*;
import com.sajilni.entity.UserEntity;
import com.sajilni.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
public class AuthenticationService {

    private final AuthenticationManager authManager;
    private final UserService userService;
    private final JwtService jwtService;
    private final MessageSource messageSource;
    private final RateLimitService rateLimitService;

    public AuthenticationService(AuthenticationManager authManager,
                                 UserService userService,
                                 JwtService jwtService,
                                 MessageSource messageSource,
                                 RateLimitService rateLimitService) {
        this.authManager = authManager;
        this.userService = userService;
        this.jwtService = jwtService;
        this.messageSource = messageSource;
        this.rateLimitService = rateLimitService;
    }

    public ResponseEntity<ApiResponse<LoginResponse>> authenticate(LoginDto dto) {
        Locale locale = LocaleContextHolder.getLocale();
        String email = dto.getEmail().toLowerCase();

        try {
            // Check rate limiting for login attempts
            if (rateLimitService.isLoginRateLimited(email)) {
                String message = messageSource.getMessage("login.attempts.exceeded", null,
                        "Too many login attempts. Please try again later.", locale);
                throw new BusinessException("login.rate.limited", message, HttpStatus.TOO_MANY_REQUESTS);
            }

            // Authenticate user
            Authentication auth = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, dto.getPassword()));

            if (!auth.isAuthenticated()) {
                rateLimitService.recordFailedLoginAttempt(email);
                throw new BadCredentialsException("Authentication failed");
            }

            // Get user details
            UserEntity userEntity = userService.findByEmailOrThrow(email);

            // Generate tokens
            Map<String, Object> claims = buildTokenClaims(userEntity);
            String accessToken = jwtService.generateToken(email, userEntity.getId(), claims);
            String refreshToken = jwtService.generateRefreshToken(email, userEntity.getId());

            // Calculate expiration
            Duration expiration = jwtService.getTokenExpiration();

            // Reset failed attempts on successful login
            rateLimitService.resetFailedLoginAttempts(email);

            LoginResponse response = LoginResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .expiresIn(expiration.toSeconds())
                    .tokenType("Bearer")
                    .user(UserInfo.builder()
                            .id(userEntity.getId())
                            .email(userEntity.getEmail())
                            .firstName(userEntity.getFirstName())
                            .lastName(userEntity.getLastName())
                            .enabled(userEntity.isEnabled())
                            .build())
                    .build();

            String message = messageSource.getMessage("login.successful", null,
                    "Login successful", locale);

            log.info("User authenticated successfully: {}", email);

            return ResponseEntity.ok(ApiResponse.<LoginResponse>builder()
                    .success(true)
                    .message(message)
                    .data(response)
                    .build());

        } catch (DisabledException e) {
            rateLimitService.recordFailedLoginAttempt(email);
            String message = messageSource.getMessage("auth.unverified", null,
                    "Please verify your email before logging in", locale);
            throw new BusinessException("auth.unverified", message, HttpStatus.FORBIDDEN);

        } catch (LockedException e) {
            String message = messageSource.getMessage("account.locked", null,
                    "Account is locked", locale);
            throw new BusinessException("account.locked", message, HttpStatus.LOCKED);

        } catch (BadCredentialsException e) {
            rateLimitService.recordFailedLoginAttempt(email);
            String message = messageSource.getMessage("auth.bad", null,
                    "Invalid email or password", locale);
            throw new BusinessException("auth.bad", message, HttpStatus.UNAUTHORIZED);

        } catch (AuthenticationException e) {
            rateLimitService.recordFailedLoginAttempt(email);
            String message = messageSource.getMessage("auth.failed", null,
                    "Authentication failed", locale);
            throw new BusinessException("auth.failed", message, HttpStatus.UNAUTHORIZED);
        }
    }

    public ResponseEntity<ApiResponse<TokenResponse>> refreshToken(RefreshTokenDto dto) {
        Locale locale = LocaleContextHolder.getLocale();

        try {
            String refreshToken = dto.getRefreshToken();

            // Validate refresh token
            if (!jwtService.validateToken(refreshToken) || !jwtService.isRefreshToken(refreshToken)) {
                String message = messageSource.getMessage("jwt.invalid", null,
                        "Invalid refresh token", locale);
                throw new BusinessException("jwt.invalid", message, HttpStatus.UNAUTHORIZED);
            }

            // Extract user information
            String email = jwtService.getEmailFromToken(refreshToken);
            Long userId = jwtService.getUserIdFromToken(refreshToken);

            // Get user details to ensure user still exists and is active
            UserEntity userEntity = userService.findByEmailOrThrow(email);

            if (!userEntity.isEnabled()) {
                String message = messageSource.getMessage("auth.unverified", null,
                        "Account is not verified", locale);
                throw new BusinessException("auth.unverified", message, HttpStatus.FORBIDDEN);
            }

            // Generate new access token
            Map<String, Object> claims = buildTokenClaims(userEntity);
            String newAccessToken = jwtService.generateToken(email, userId, claims);

            Duration expiration = jwtService.getTokenExpiration();

            TokenResponse response = TokenResponse.builder()
                    .accessToken(newAccessToken)
                    .expiresIn(expiration.toSeconds())
                    .tokenType("Bearer")
                    .build();

            String message = messageSource.getMessage("token.refreshed", null,
                    "Token refreshed successfully", locale);

            log.info("Token refreshed for user: {}", email);

            return ResponseEntity.ok(ApiResponse.<TokenResponse>builder()
                    .success(true)
                    .message(message)
                    .data(response)
                    .build());

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Token refresh failed", e);
            String message = messageSource.getMessage("jwt.invalid", null,
                    "Invalid refresh token", locale);
            throw new BusinessException("jwt.invalid", message, HttpStatus.UNAUTHORIZED);
        }
    }

    private Map<String, Object> buildTokenClaims(UserEntity user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("firstName", user.getFirstName());
        claims.put("lastName", user.getLastName());
        claims.put("enabled", user.isEnabled());
        // Add any additional claims needed
        return claims;
    }
}