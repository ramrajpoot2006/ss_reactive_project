package com.domainname.next.shippingapi.exception;

import lombok.Getter;

@Getter
public class NotFoundException extends ShippingApiException {

  private static final long serialVersionUID = 1L;

  public NotFoundException(Throwable cause, String messageCode, String... args) {
    super(cause, messageCode, args);
  }
  
  public NotFoundException(String messageCode, String... args) {
    super(messageCode, args);
  }
}
