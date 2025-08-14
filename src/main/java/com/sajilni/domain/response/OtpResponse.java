package com.sajilni.domain.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OtpResponse {
    private String email;
    private int remainingAttempts;
}
