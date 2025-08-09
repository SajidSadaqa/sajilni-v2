// src/main/java/com/sajilni/dto/RegisterDto.java
package com.sajilni.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

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

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getOsName() { return osName; }
    public void setOsName(String osName) { this.osName = osName; }
    public String getOsVersion() { return osVersion; }
    public void setOsVersion(String osVersion) { this.osVersion = osVersion; }
    public String getClientTimestamp() { return clientTimestamp; }
    public void setClientTimestamp(String clientTimestamp) { this.clientTimestamp = clientTimestamp; }
}
