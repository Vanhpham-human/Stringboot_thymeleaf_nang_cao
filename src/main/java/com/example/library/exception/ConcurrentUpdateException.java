package com.example.library.exception;

/** Xung đột optimistic locking: dữ liệu đã bị request khác thay đổi trong lúc xử lý. */
public class ConcurrentUpdateException extends BusinessException {

    public ConcurrentUpdateException(String message) {
        super(message);
    }
}
