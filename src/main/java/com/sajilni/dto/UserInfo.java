package com.sajilni.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserInfo {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private boolean enabled;
}
