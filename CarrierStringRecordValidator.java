package com.domainname.next.shippingapi.validator;

import java.util.List;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.springframework.util.CollectionUtils;

import com.domainname.next.shippingapi.annotation.CarrierStringRecordPattern;
import com.domainname.next.shippingapi.resources.response.CarrierStringRecord;

public class CarrierStringRecordValidator
    implements ConstraintValidator<CarrierStringRecordPattern, List<CarrierStringRecord>> {

  @Override
  public boolean isValid(List<CarrierStringRecord> value, ConstraintValidatorContext context) {
    if (value == null) {
       return true;
    }
    return validateCarrierStringRecordValue(value);
  }

  private boolean validateCarrierStringRecordValue(List<CarrierStringRecord> value) {
    if(CollectionUtils.isEmpty(value)) {
      return false;
    }
    for (int i = 0; i < value.size(); i++) {
      if (value.get(i) == null) {
        return false;
      }
    }
    return true;
  }
}
