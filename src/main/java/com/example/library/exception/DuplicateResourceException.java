package com.example.library.exception;

public class DuplicateResourceException extends BusinessException {

    public DuplicateResourceException(String field, String message) {
        super(field, message);
    }
}
