package com.pitstop.pitstop_parts.part.exception;

public class PartNotFoundException extends RuntimeException {

    public PartNotFoundException(String message) {
        super(message);
    }
}