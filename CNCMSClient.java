package com.domainname.next.shippingapi.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.domainname.next.shippingapi.client.request.CNCMSRequest;
import com.domainname.next.shippingapi.client.response.cnc.CNCMSResponse;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Component
@Slf4j
@ConfigurationProperties(prefix = "cnc-ms")
public class CNCMSClient extends Client {

  @Autowired
  @Qualifier("dpeWebClient")
  private WebClient webClient;

  @Value("${cnc-ms.x-api-key}")
  private String xApiKey;

  public Mono<CNCMSResponse> getCNCDPE(
      CNCMSRequest cncDPERequest, String channel,
      String enterpriseCode) {
    log.info("Calling CNC DPE Microservice endpoint {}", cncDPERequest.getCountry());
    return webClient.post()
        .uri(UriComponentsBuilder.fromHttpUrl(host).path(uri).build().toUri())
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .header("x-api-key", xApiKey)
        .header("channel", channel)
        .header("enterprise-code", enterpriseCode)
        .body(Mono.just(cncDPERequest), CNCMSRequest.class)
        .retrieve()
        .onStatus(HttpStatus::isError, response -> {
          log.error("Error from CNC DPE Microservice endpoint {}", response.statusCode());
          return response.createException().flatMap(Mono::error);
        }).bodyToMono(CNCMSResponse.class)
        .doOnError(throwable -> log.error("Error in getting CNC DPE response : {}",
            throwable.getMessage()))
        .doOnSuccess(cncResponse -> log.info("Received CNC DPE response : {}",
            cncResponse.getReferenceId()));
  }
}
