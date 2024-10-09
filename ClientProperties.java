package com.domainname.next.shippingapi.client.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "client")
public class ClientProperties {

  private Long connectTimeout;
  private Long idleTimeout;
  private Long responseTimeout;
  private Integer retries;
  private Integer retryDelayMilis;
  private Long dpeResponseTimeout;
}
