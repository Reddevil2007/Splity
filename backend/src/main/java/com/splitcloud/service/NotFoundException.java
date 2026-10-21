package com.splitcloud.service;

/** Thrown when a requested group, member or expense does not exist. Mapped to HTTP 404. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
