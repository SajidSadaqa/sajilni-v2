package com.sajilni.domain.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterReq {
    @NotBlank(message = "First name is required")
    private String firstName;
    @NotBlank(message = "Last Name is required")
    private String lastName;
    @Email
    @NotBlank private String email;
    @Size(min = 8) @NotBlank private String password;
    @Size(min = 8) @NotBlank private String confirmPassword;

    // Accept patterns from the user: @Pattern(regexp = "^([a-zA-Z0-9_\\-\\.]+)@([a-zA-Z0-9_\\-\\.]+)\\.([a-zA-Z]{2,5})$")

    private String platform;
    private String serialNumber;
    private String model;
    private String osName;
    private String osVersion;
    private String clientTimestamp;
}
