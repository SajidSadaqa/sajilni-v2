package com.sajilni.domain.response;

import com.sajilni.dto.UserInfo;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private Long expiresIn;
    private String tokenType = "Bearer";
    private UserInfo user;
}
