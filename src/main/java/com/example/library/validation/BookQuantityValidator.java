package com.example.library.validation;

import com.example.library.dto.form.BookForm;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class BookQuantityValidator implements ConstraintValidator<ValidBookQuantity, BookForm> {

    @Override
    public boolean isValid(BookForm form, ConstraintValidatorContext context) {
        if (form == null || form.getTotalQuantity() == null || form.getAvailableQuantity() == null) {
            return true;
        }
        if (form.getAvailableQuantity() <= form.getTotalQuantity()) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("availableQuantity")
                .addConstraintViolation();
        return false;
    }
}
