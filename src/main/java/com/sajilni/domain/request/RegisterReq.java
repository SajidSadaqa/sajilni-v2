package com.sajilni.domain.request;

import com.sajilni.validation.PasswordMatch;
import com.sajilni.validation.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@PasswordMatch(password = "password", confirmPassword = "confirmPassword")
public class RegisterReq {

    @NotBlank(message = "{validation.firstname.required}")
    @Size(min = 2, max = 50, message = "{validation.firstname.size}")
    private String firstName;

    @NotBlank(message = "{validation.lastname.required}")
    @Size(min = 2, max = 50, message = "{validation.lastname.size}")
    private String lastName;

    @Email(message = "{validation.email.invalid}")
    @NotBlank(message = "{validation.email.required}")
    private String email;

    @StrongPassword(minLength = 8)
    @NotBlank(message = "{validation.password.required}")
    private String password;

    @NotBlank(message = "{validation.confirm.password.required}")
    private String confirmPassword;

    // Device metadata (optional)
    @Size(max = 32, message = "{validation.platform.size}")
    private String platform;

    @Size(max = 128, message = "{validation.serial.size}")
    private String serialNumber;

    @Size(max = 128, message = "{validation.model.size}")
    private String model;

    @Size(max = 64, message = "{validation.os.name.size}")
    private String osName;

    @Size(max = 64, message = "{validation.os.version.size}")
    private String osVersion;

    @Size(max = 64, message = "{validation.client.timestamp.size}")
    private String clientTimestamp;
}