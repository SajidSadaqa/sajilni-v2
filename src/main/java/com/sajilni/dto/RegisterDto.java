// src/main/java/com/sajilni/dto/RegisterDto.java
package com.sajilni.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterDto {
    @NotBlank private String firstName;
    @NotBlank private String lastName;
    @Email @NotBlank private String email;
    @Size(min = 8) @NotBlank private String password;
    @Size(min = 8) @NotBlank private String confirmPassword;

    // optional device metadata
    private String platform;
    private String serialNumber;
    private String model;
    private String osName;
    private String osVersion;
    private String clientTimestamp;

}
