package com.example.library.validation;

import com.example.library.dto.form.BorrowingForm;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class BorrowingDatesValidator implements ConstraintValidator<ValidBorrowingDates, BorrowingForm> {

    @Override
    public boolean isValid(BorrowingForm form, ConstraintValidatorContext context) {
        if (form == null || form.getBorrowDate() == null || form.getDueDate() == null) {
            return true;
        }
        if (form.getDueDate().isAfter(form.getBorrowDate())) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("dueDate")
                .addConstraintViolation();
        return false;
    }
}
