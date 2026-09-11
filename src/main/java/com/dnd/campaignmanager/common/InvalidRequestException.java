package com.dnd.campaignmanager.common;

import lombok.Getter;

import java.util.Map;

@Getter
public class InvalidRequestException extends RuntimeException {

    private final Map<String, String> fieldErrors;

    public InvalidRequestException(String message) {
        this(message, Map.of());
    }

    public InvalidRequestException(String message, Map<String, String> fieldErrors) {
        super(message);
        this.fieldErrors = fieldErrors;
    }
}
