package com.sajilni.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshTokenDto {
    @NotBlank(message = "{validation.refresh.token.required}")
    private String refreshToken;
}
