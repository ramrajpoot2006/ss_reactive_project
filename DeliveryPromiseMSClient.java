package com.domainname.next.shippingapi.client;

import com.domainname.next.shippingapi.client.request.DeliveryPromiseRequest;
import com.domainname.next.shippingapi.client.response.DeliveryPromiseResponse;
import com.domainname.next.shippingapi.enums.TargetService;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;

@Component
@Slf4j
@Getter
@Setter
@ConfigurationProperties(prefix = "deliverypromise-ms")
public class DeliveryPromiseMSClient extends Client implements DeliveryPromiseClientTargetService {

  private static final TargetService targetService = TargetService.DPEV2_HD_MS;

  @Autowired
  @Qualifier("dpeWebClient")
  private WebClient client;

  private String xApiKey;

  public Mono<DeliveryPromiseResponse> getDeliveryPromise(DeliveryPromiseRequest deliveryPromiseRequest) {
    log.info("Calling Delivery Promise Microservice {}", deliveryPromiseRequest.getOrganizationCode());
    return client
        .post()
        .uri(UriComponentsBuilder.fromHttpUrl(host)
                 .path(uri)
                 .build()
                 .toUri())
        .contentType(MediaType.APPLICATION_JSON)
        .header("x-api-key", xApiKey)
        .body(Mono.just(deliveryPromiseRequest), DeliveryPromiseRequest.class)
        .retrieve()
        .onStatus(HttpStatus::isError, response -> {
          log.error("Error from Delivery Promise Microservice Endpoint {}", response.statusCode());
          return response.createException().flatMap(Mono::error);
        })
        .bodyToMono(DeliveryPromiseResponse.class)
        .doOnError(throwable -> log.error("Error in getting Delivery Promise Microservice response : {}",
            throwable.getMessage()))
        .doOnSuccess(dPEResponse -> log.info("Received Delivery Promise Microservice response"));
  }

  public Boolean shouldApply(Map<String, Boolean> siteIdTargetEnabledMap) {
    return Optional.ofNullable(siteIdTargetEnabledMap.get(DeliveryPromiseMSClient.targetService.getValue())).orElse(false);
  }
}
