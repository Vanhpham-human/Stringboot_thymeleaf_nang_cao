package com.example.library.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Số lượng đang có không được lớn hơn tổng số lượng. */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = BookQuantityValidator.class)
public @interface ValidBookQuantity {

    String message() default "Số lượng đang có không được lớn hơn tổng số lượng";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
