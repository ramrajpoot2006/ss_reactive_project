package com.domainname.next.shippingapi.client;

import com.domainname.next.shippingapi.client.request.cnc.dpe.CNCDPERequest;
import com.domainname.next.shippingapi.client.response.cnc.dpe.CNCDPEResponse;
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
@ConfigurationProperties(prefix = "cnc-dpe")
public class CNCDPEClient extends Client {

  @Autowired
  @Qualifier("dpeWebClient")
  private WebClient webClient;

  @Value("${cnc-dpe.api-id}")
  private String apiId;

  public Mono<CNCDPEResponse> getCNCDPE(CNCDPERequest cncDPERequest) {
    log.info("Calling Click And Collect Delivery Promise Service {}", cncDPERequest.getOrganizationCode());
    return webClient
        .post()
        .uri(UriComponentsBuilder.fromHttpUrl(host)
                 .path(uri)
                 .build()
                 .toUri())
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .header("app_id", apiId)
        .body(Mono.just(cncDPERequest), CNCDPERequest.class)
        .retrieve()
        .onStatus(HttpStatus::isError, response -> {
          log.error("Error from Click And Collect DPE {}", response.statusCode());
          return response.createException().flatMap(Mono::error);
        })
        .bodyToMono(CNCDPEResponse.class)
        .doOnError(
            throwable -> log.error("Error in getting Click And Collect DPE response : {}", throwable.getMessage()))
        .doOnSuccess(
            cncResponse -> log.info("Received Click And Collect DPE response : {}", cncResponse.getReferenceId()));
  }
}
