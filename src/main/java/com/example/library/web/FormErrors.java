package com.example.library.web;

import com.example.library.exception.BusinessException;
import org.springframework.validation.BindingResult;

/** Gắn lỗi nghiệp vụ từ service vào BindingResult để hiển thị ngay trên form. */
public final class FormErrors {

    private FormErrors() {
    }

    public static void apply(BindingResult bindingResult, BusinessException ex) {
        String field = ex.getField();
        if (field != null && bindingResult.getTarget() != null
                && isWritable(bindingResult, field)) {
            bindingResult.rejectValue(field, "business", ex.getMessage());
        } else {
            bindingResult.reject("business", ex.getMessage());
        }
    }

    private static boolean isWritable(BindingResult bindingResult, String field) {
        try {
            return bindingResult.getFieldType(field) != null;
        } catch (RuntimeException e) {
            return false;
        }
    }
}
