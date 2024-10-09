package com.domainname.next.shippingapi.client;

import com.domainname.next.shippingapi.client.request.DeliveryPromiseRequest;
import com.domainname.next.shippingapi.client.response.DeliveryPromiseResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

@Component
@Slf4j
@ConfigurationProperties(prefix = "dpe-failover")
public class DPEFailoverClient extends Client {

  @Value("${dpe-failover.appid}")
  private String appId;

  @Autowired
  @Qualifier("dpeWebClient")
  private WebClient webClient;

  public Mono<DeliveryPromiseResponse> getDeliveryPromiseFailover(DeliveryPromiseRequest deliveryPromiseRequest) {
    log.info("Calling Delivery Promise Failover Endpoint {}", deliveryPromiseRequest.getOrganizationCode());
    return webClient
        .post()
        .uri(UriComponentsBuilder.fromHttpUrl(host)
                 .path(uri)
                 .build()
                 .toUri())
        .contentType(MediaType.APPLICATION_JSON) 
        .header("api-key", appId)
        .body(Mono.just(deliveryPromiseRequest), DeliveryPromiseRequest.class)
        .retrieve()
        .onStatus(HttpStatus::isError, response -> {
          log.error("Error from Delivery Promise Failover Endpoint {}", response.statusCode());
          return response.createException().flatMap(Mono::error);
        })
        .bodyToMono(DeliveryPromiseResponse.class)
        .doOnError(throwable -> log.error("Error in getting Delivery Promise Failover response : {}", throwable.getMessage()))
        .doOnSuccess(dPEResponse -> log.info("Received Delivery Promise Failover response")); 
  }
}
