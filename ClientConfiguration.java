package com.domainname.next.shippingapi.client.configuration;


import com.domainname.next.shippingapi.filter.MDCManager;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.JettyClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactivefeign.jetty.JettyHttpClientFactory;
import reactivefeign.spring.config.EnableReactiveFeignClients;
import reactor.util.retry.Retry;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;

@Configuration
@Setter
@Slf4j
@EnableReactiveFeignClients
public class ClientConfiguration {

  @Autowired
  private ClientProperties properties;
  
  @Autowired
  WebClient.Builder webClientBuilder;

  @Autowired
  private MDCManager mdcManager;

  @Autowired
  private CircuitBreakerRegistry circuitBreakerRegistry;

  @Bean
  JettyHttpClientFactory clientFactory(HttpClient httpClient) {
    return useHttp2 -> httpClient;
  }

  @Bean
  HttpClient httpClient(MeterRegistry registry) {
    HttpClient httpClient = new HttpClient(new SslContextFactory.Client()){
      @Override
      public Request newRequest(
          URI uri) {
        Request request = super.newRequest(uri);
        return enhanceRequest(request);
      }
    };
    httpClient.setConnectTimeout(properties.getConnectTimeout());
    httpClient.setIdleTimeout(properties.getIdleTimeout());
    TaggedCircuitBreakerMetrics.ofCircuitBreakerRegistry(circuitBreakerRegistry).bindTo(registry);
    return httpClient;
  }

  private Request enhanceRequest(Request inboundRequest) {
    long milis = System.currentTimeMillis();
    inboundRequest.onResponseContent((response, content) -> mdcManager.insertOutgoingClientResponse(response, content));
    inboundRequest.onResponseSuccess(listener -> {
      mdcManager.insertOutgoingClientRequest(inboundRequest);
      log.info("Finished response to 3rd party service within {} millis", System.currentTimeMillis() - milis);
      mdcManager.cleanupOutgoingRequestFields();
    });
    inboundRequest.onResponseFailure((listener, failure) -> {
      mdcManager.insertOutgoingClientRequest(inboundRequest);
      log.error("Error response to 3rd party service ", failure);
      mdcManager.cleanupOutgoingRequestFields();
    });
    return inboundRequest;
  }

  @Bean
  @Primary
  public WebClient buildClient (HttpClient httpClient) {
    JettyClientHttpConnector connector = new JettyClientHttpConnector(httpClient);
    return webClientBuilder
        .clientConnector(connector)
        .filter(responseTimeout(properties.getResponseTimeout()))
        .filter(retryFilter())
        .build();
  }

  @Bean("dpeWebClient")
  public WebClient buildDPEClient (HttpClient httpClient) {
    ClientHttpConnector connector = new JettyClientHttpConnector(httpClient);
    return webClientBuilder
        .clientConnector(connector)
        .filter(responseTimeout(properties.getDpeResponseTimeout()))
        .filter(retryFilter())
        .build();
  }

  // This method returns filter function which will add a timeout
  private static ExchangeFilterFunction responseTimeout(Long responseTimeout) {
    return (request, next) -> next.exchange(request).timeout(Duration.ofMillis(responseTimeout));
  }

  private ExchangeFilterFunction retryFilter() {
    return (request, next) ->
        next.exchange(request)
            .retryWhen(
                Retry.fixedDelay(properties.getRetries(), Duration.ofMillis(properties.getRetryDelayMilis()))
                    .doAfterRetry(retrySignal -> log.warn("Retrying request")));
  }
}
