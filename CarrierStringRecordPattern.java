package com.domainname.next.shippingapi.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import com.domainname.next.shippingapi.resources.response.CarrierStringRecord;
import com.domainname.next.shippingapi.validator.CarrierStringRecordValidator;

@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CarrierStringRecordValidator.class)
public @interface CarrierStringRecordPattern {
  Class<?>[] groups() default {};
  Class<? extends Payload>[] payload() default {};
  Class<CarrierStringRecord> targetClassType();
  String message() default "";
}
