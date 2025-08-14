package com.sajilni.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OtpVerificationResponse {
    private boolean verified;
    private String email;
    private Long userId;
    private int remainingAttempts;
}
