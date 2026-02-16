package com.akibaplus.saccos.akibaplus.exception;

import java.time.Instant;

public class ApiError {
    private Instant timestamp = Instant.now();
    private int status;
    private String message;

    public ApiError(int status, String message) {
        this.status = status;
        this.message = message;
    }

    public Instant getTimestamp() { return timestamp; }
    public int getStatus() { return status; }
    public String getMessage() { return message; }
}