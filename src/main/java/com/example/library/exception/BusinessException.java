package com.example.library.exception;

import lombok.Getter;

/**
 * Lỗi vi phạm quy tắc nghiệp vụ. Nếu {@code field} khác null, lỗi gắn với một trường cụ thể trên form.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final String field;

    public BusinessException(String message) {
        this(null, message);
    }

    public BusinessException(String field, String message) {
        super(message);
        this.field = field;
    }

    public boolean hasField() {
        return field != null && !field.isBlank();
    }
}
