package com.manocorbas.ciphermq.exceptions;

public class NonExistentTopicException extends Exception {

    public NonExistentTopicException() {
        super("Topic does not exist");
    }

    public NonExistentTopicException(String message) {
        super(message);
    }
    
}
