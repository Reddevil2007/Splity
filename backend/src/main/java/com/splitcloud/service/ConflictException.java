package com.splitcloud.service;

/** Thrown when a request conflicts with existing data, e.g. deleting a person who has expenses. Mapped to HTTP 409. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
