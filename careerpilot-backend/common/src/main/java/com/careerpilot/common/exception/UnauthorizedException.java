package com.careerpilot.common.exception;

/** Mirrors CareerPilot.Shared.Exceptions.UnauthorizedException -> HTTP 401. */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
