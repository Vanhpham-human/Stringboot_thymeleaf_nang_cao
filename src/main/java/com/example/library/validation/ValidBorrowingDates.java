package com.example.library.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Hạn trả (nếu nhập) phải sau ngày mượn. */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = BorrowingDatesValidator.class)
public @interface ValidBorrowingDates {

    String message() default "Hạn trả phải sau ngày mượn";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
