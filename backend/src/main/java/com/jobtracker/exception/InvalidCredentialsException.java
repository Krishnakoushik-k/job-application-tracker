package com.jobtracker.exception;

/** Same message for "unknown email" and "wrong password" so accounts cannot be enumerated. */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
}
