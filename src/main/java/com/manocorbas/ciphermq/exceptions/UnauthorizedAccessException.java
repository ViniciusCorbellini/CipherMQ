package com.manocorbas.ciphermq.exceptions;

public class UnauthorizedAccessException extends Exception{

    public UnauthorizedAccessException() {
        super("Access Unauthorized");
    }

    public UnauthorizedAccessException(String message) {
        super(message);
    }
    
}
