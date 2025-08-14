package com.sajilni.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VerifyOtpDto {

    @Email(message = "{validation.email.invalid}")
    @NotBlank(message = "{validation.email.required}")
    private String email;

    @NotBlank(message = "{validation.otp.required}")
    @Pattern(regexp = "^\\d{6}$", message = "{validation.otp.format}")
    private String otp;
}