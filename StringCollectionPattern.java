package com.domainname.next.shippingapi.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import com.domainname.next.shippingapi.validator.StringCollectionValidator;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = StringCollectionValidator.class)
public @interface StringCollectionPattern {
  Class<?>[] groups() default {};
  Class<? extends Payload>[] payload() default {};
  Class<? extends String> classType() default String.class;
  String message() default "";
  int maxLength();
}
