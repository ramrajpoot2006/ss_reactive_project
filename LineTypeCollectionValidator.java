package com.domainname.next.shippingapi.validator;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.domainname.next.shippingapi.annotation.LineTypeCollectionPattern;

public class LineTypeCollectionValidator implements ConstraintValidator<LineTypeCollectionPattern, List<String>> {

  private Set<String> allowedValues;

  @Override
  public void initialize(LineTypeCollectionPattern targetEnum) {
    var enumSelected = targetEnum.targetClassType().getEnumConstants();
    allowedValues = Stream.of(enumSelected).map(Object::toString).collect(Collectors.toSet());
  }

  @Override
  public boolean isValid(List<String> value, ConstraintValidatorContext context) {
    if (value == null) {
      return true;
    }
    int counter = 0;
    int count = value.size();
    for (String allowedValue : allowedValues) {
      for (String val : value) {
        if (allowedValue.equals(val)) {
          counter++;
        }
      }
    }
    return count == counter;
  }
}
