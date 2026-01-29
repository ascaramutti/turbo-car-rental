package com.turbo.exception;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class ValidationErrorResponse extends ErrorResponse {

    private Map<String, String> details;

    public ValidationErrorResponse(String code, String message, int status, String timestamp,
                                   Map<String, String> details) {
        super(code, message, status, timestamp);
        this.details = details;
    }
}
