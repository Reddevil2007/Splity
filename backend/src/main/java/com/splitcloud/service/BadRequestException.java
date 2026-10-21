package com.splitcloud.service;

/** Thrown for input that is well-formed JSON but breaks a business rule. Mapped to HTTP 400. */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
