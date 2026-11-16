package com.jobtracker.exception;

/**
 * Thrown when a resource does not exist OR belongs to another user. Both cases return 404 on purpose,
 * so attackers cannot discover which ids exist (prevents insecure direct object reference leaks).
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
