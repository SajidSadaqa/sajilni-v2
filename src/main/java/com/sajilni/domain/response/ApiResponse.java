package com.sajilni.domain.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private String timestamp = java.time.LocalDateTime.now().toString();
}




