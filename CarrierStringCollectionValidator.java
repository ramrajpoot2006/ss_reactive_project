package com.domainname.next.shippingapi.validator;

import java.util.List;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.logging.log4j.util.Strings;

import com.domainname.next.shippingapi.annotation.CarrierStringCollectionPattern;

public class CarrierStringCollectionValidator
    implements ConstraintValidator<CarrierStringCollectionPattern, List<String>> {
  
  private int length;
  
  @Override
  public void initialize(CarrierStringCollectionPattern targetEnum) {
    length = targetEnum.maxLength();
  }

  @Override
  public boolean isValid(List<String> value, ConstraintValidatorContext context) {
    if (value == null) {
      return true;
    }
    for (int i = 0; i < value.size(); i++) {
      if (Strings.isBlank(value.get(i)) || value.get(i).length() > length) {
        return false;
      }
    }
    return true;
  }
}
