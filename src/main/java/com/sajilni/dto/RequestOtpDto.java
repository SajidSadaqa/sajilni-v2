package com.sajilni.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RequestOtpDto {
    @Email(message = "{validation.email.invalid}")
    @NotBlank(message = "{validation.email.required}")
    private String email;
}