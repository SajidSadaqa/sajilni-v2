package com.sajilni.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeneralResponse<T> {

    private Integer code;
    private String message;
    private T data;

}
