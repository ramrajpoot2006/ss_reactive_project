package com.domainname.next.shippingapi.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import com.domainname.next.shippingapi.validator.UUIDCollectionValidator;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = UUIDCollectionValidator.class)
public @interface UUIDCollectionPattern {
  Class<?>[] groups() default {};
  Class<? extends Payload>[] payload() default {};
  Class<? extends String> classType() default String.class;
  String message() default "";
}
