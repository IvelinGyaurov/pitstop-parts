package com.pitstop.pitstop_parts.part.exception;

public class SkuAlreadyExistsException extends RuntimeException {

    public SkuAlreadyExistsException(String message) {
        super(message);
    }
}