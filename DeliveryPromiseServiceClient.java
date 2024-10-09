package com.domainname.next.shippingapi.client;

import com.domainname.next.shippingapi.client.request.DeliveryPromiseRequest;
import com.domainname.next.shippingapi.client.response.DeliveryPromiseResponse;
import com.domainname.next.shippingapi.enums.TargetService;
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

import java.util.Map;
import java.util.Optional;

@Component
@Slf4j

@ConfigurationProperties(prefix = "deliverypromise")
public class DeliveryPromiseServiceClient extends Client implements DeliveryPromiseClientTargetService {

  private static final TargetService targetService = TargetService.DPEV2_HD_MS;

  @Value("${deliverypromise.appid}")
  private String appId;

  @Autowired
  @Qualifier("dpeWebClient")
  private WebClient webClient;

  public Mono<DeliveryPromiseResponse> getDeliveryPromise(DeliveryPromiseRequest deliveryPromiseRequest) {
    log.info("Calling Delivery Promise Service {}", deliveryPromiseRequest.getOrganizationCode());
    return webClient
        .post()
        .uri(UriComponentsBuilder.fromHttpUrl(host)
                 .path(uri)
                 .build()
                 .toUri())
        .contentType(MediaType.APPLICATION_JSON)
        .header("app_id", appId)
        .body(Mono.just(deliveryPromiseRequest), DeliveryPromiseRequest.class)
        .retrieve()
        .onStatus(HttpStatus::isError, response -> {
          log.error("Error from Delivery Promise Endpoint {}", response.statusCode());
          return response.createException().flatMap(Mono::error);
        })
        .bodyToMono(DeliveryPromiseResponse.class)
        .doOnError(throwable -> log.error("Error in getting Delivery Promise response : {}", throwable.getMessage()))
        .doOnSuccess(dPEResponse -> log.info("Received Delivery Promise response"));
  }

  public Boolean shouldApply(Map<String, Boolean> siteIdTargetEnabledMap) {
    return !Optional.ofNullable(siteIdTargetEnabledMap.get(DeliveryPromiseServiceClient.targetService.getValue())).orElse(false);
  }
}
