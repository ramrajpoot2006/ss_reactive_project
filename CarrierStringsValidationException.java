package com.domainname.next.shippingapi.exception;

import lombok.Getter;

@Getter
public class CarrierStringsValidationException extends ShippingApiException {
  
  private static final long serialVersionUID = 1L;

  public CarrierStringsValidationException(Throwable cause, String messageCode, String... args) {
    super(cause, messageCode, args);
  }
}
