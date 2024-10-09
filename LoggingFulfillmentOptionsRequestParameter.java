package com.domainname.next.shippingapi.filter;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class LoggingFulfillmentOptionsRequestParameter {
  private String siteId;
  private String channel;
  private String basketReferenceId;
}
