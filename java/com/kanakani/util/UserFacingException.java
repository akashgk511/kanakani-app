package com.kanakani.util;

/** An error whose message is safe and useful to show to the person, for example "That code is not right." */
public class UserFacingException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public UserFacingException(String message) {
        super(message);
    }
}
