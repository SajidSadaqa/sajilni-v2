package com.sajilni.controller;

import com.sajilni.domain.request.OtpRequest;
import com.sajilni.domain.request.RegisterReq;
import com.sajilni.domain.response.*;
import com.sajilni.dto.*;
import com.sajilni.service.AuthFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping(value = "/api/auth", produces = "application/json;charset=UTF-8")
@Tag(name = "Authentication", description = "User authentication and registration endpoints")
public class AuthController {

    private final AuthFacade authFacade;

    public AuthController(AuthFacade authFacade) {
        this.authFacade = authFacade;
    }

    @PostMapping("/register")
    @Operation(
            summary = "Register a new user",
            description = "Creates a new user account and sends an OTP verification email"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "User registered successfully",
                    content = @Content(schema = @Schema(implementation = RegisterResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input data or user already exists",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "429",
                    description = "Too many registration attempts",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<com.sajilni.domain.response.ApiResponse<RegisterResponse>> register(
            @Valid @RequestBody RegisterReq registerReq,
            @Parameter(hidden = true) @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {

        log.info("Registration attempt for email: {}", registerReq.getEmail());
        return authFacade.register(registerReq);
    }

    @PostMapping("/verify-otp")
    @Operation(
            summary = "Verify OTP code",
            description = "Verifies the OTP code sent to user's email and activates the account"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "OTP verified successfully",
                    content = @Content(schema = @Schema(implementation = OtpVerificationResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid or expired OTP",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Too many failed OTP attempts - account locked",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<com.sajilni.domain.response.ApiResponse<OtpVerificationResponse>> verifyOtp(
            @Valid @RequestBody VerifyOtpDto dto,
            @Parameter(hidden = true) @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {

        log.info("OTP verification attempt for email: {}", dto.getEmail());
        return authFacade.verifyOtp(dto);
    }

    @PostMapping("/login")
    @Operation(
            summary = "User login",
            description = "Authenticates user and returns access and refresh tokens"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Login successful",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid credentials",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Account not verified or locked",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "429",
                    description = "Too many login attempts",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<com.sajilni.domain.response.ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginDto dto,
            @Parameter(hidden = true) @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {

        log.info("Login attempt for email: {}", dto.getEmail());
        return authFacade.login(dto);
    }

    @PostMapping("/request-otp")
    @Operation(
            summary = "Request new OTP",
            description = "Sends a new OTP code to the user's email"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "New OTP sent successfully",
                    content = @Content(schema = @Schema(implementation = OtpResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "User not found or already verified",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "429",
                    description = "Too many OTP requests",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<com.sajilni.domain.response.ApiResponse<OtpResponse>> requestNewOtp(
            @Valid @RequestBody OtpRequest dto,
            @Parameter(hidden = true) @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {

        log.info("New OTP requested for email: {}", dto.getEmail());
        return authFacade.requestNewOtp(dto);
    }

    @PostMapping("/refresh-token")
    @Operation(
            summary = "Refresh access token",
            description = "Generates a new access token using a valid refresh token"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Token refreshed successfully",
                    content = @Content(schema = @Schema(implementation = TokenResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid or expired refresh token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<com.sajilni.domain.response.ApiResponse<TokenResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenDto dto,
            @Parameter(hidden = true) @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {

        log.info("Token refresh requested");
        return authFacade.refreshToken(dto);
    }

    @PostMapping("/logout")
    @Operation(
            summary = "User logout",
            description = "Invalidates the current session (client should discard tokens)"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Logout successful",
                    content = @Content(schema = @Schema(implementation = com.sajilni.domain.response.ApiResponse.class))
            )
    })
    public ResponseEntity<com.sajilni.domain.response.ApiResponse<Void>> logout(
            @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // In a stateless JWT system, logout is primarily handled client-side
        // by discarding tokens. For added security, you could maintain a blacklist
        // of invalidated tokens until their expiration.

        log.info("User logout requested");

        return ResponseEntity.ok(com.sajilni.domain.response.ApiResponse.<Void>builder()
                .success(true)
                .message("Logout successful")
                .build());
    }
}