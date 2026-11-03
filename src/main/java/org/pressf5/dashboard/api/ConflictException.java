package org.pressf5.dashboard.api;

public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
