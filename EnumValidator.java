package com.domainname.next.shippingapi.validator;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.logging.log4j.util.Strings;

import com.domainname.next.shippingapi.annotation.EnumPattern;

public class EnumValidator implements ConstraintValidator<EnumPattern, String> {

  private Set<String> allowedValues;

  @Override
  public void initialize(EnumPattern targetEnum) {
    var enumSelected = targetEnum.targetClassType().getEnumConstants();
    allowedValues = Stream.of(enumSelected)
        .map(Object::toString).collect(Collectors.toSet());
  }

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    if (Strings.isBlank(value)) {
      return true;
    }
    return allowedValues.contains(value);
  }

}
