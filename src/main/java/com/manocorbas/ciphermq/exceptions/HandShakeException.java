package com.manocorbas.ciphermq.exceptions;

public class HandShakeException extends Exception {

    public HandShakeException() {
        super("Error while performing HandShake");
    }

    public HandShakeException(String message) {
        super(message);
    }
    
}
