package com.careerpilot.common.exception;

/** Mirrors CareerPilot.Shared.Exceptions.NotFoundException -> HTTP 404. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
