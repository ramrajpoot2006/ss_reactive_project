package com.domainname.next.shippingapi.filter;


import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 *
 */
@Component
@Slf4j
@AllArgsConstructor
public class LoggingFilter implements WebFilter, Ordered {

  private final MDCManager mdcManager;

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE + 1;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    return exchange.getRequest().getPath().value().contains("/actuator") ? chain.filter(exchange)
        : processLoggingRequest(exchange, chain);
  }

  private Mono<Void> processLoggingRequest(ServerWebExchange exchange, WebFilterChain chain) {
    ServerHttpRequest request = exchange.getRequest();

    if (request.getHeaders().containsKey("api-key")) {
      request.mutate().header("api-key", "************");
    }

    final StringBuilder cachedResponse = new StringBuilder();
    ServerWebExchange mutatedServerWebExchange = exchange.mutate()
        .response(getCachingResponseDecorator(exchange.getResponse(), cachedResponse))
        .build();
    mdcManager.insertRequestMDC(exchange);
    StopWatch stopWatch = new StopWatch();
    stopWatch.start();

    return chain.filter(mutatedServerWebExchange)
        .doFinally((SignalType signalType) ->
          logResponse(exchange, cachedResponse, stopWatch)
        );
  }

  private void logResponse (ServerWebExchange exchange, StringBuilder cachedResponse, StopWatch stopWatch) {
    stopWatch.stop();
    mdcManager.insertResponseMDC(exchange, cachedResponse);
    logResponseBasedOnStatus(exchange, stopWatch);
    mdcManager.clearMDC();
    cachedResponse.setLength(0);
  }

  private void logResponseBasedOnStatus (ServerWebExchange exchange, StopWatch stopWatch) {
    if (exchange.getResponse().getStatusCode() != null) {
      var logMessage = "Request executed in {} ms";
      if (exchange.getResponse().getStatusCode().is4xxClientError()) {
        log.warn(
            logMessage,
            stopWatch.getTotalTimeMillis()
        );
      } else if (exchange.getResponse().getStatusCode().is5xxServerError()) {
        log.error(
            logMessage,
            stopWatch.getTotalTimeMillis()
        );
      } else if (log.isDebugEnabled()) {
        log.debug(
            logMessage,
            stopWatch.getTotalTimeMillis()
        );
      } else if (mdcManager.isLogBody) {
        log.info(
            logMessage,
            stopWatch.getTotalTimeMillis()
        );
      }
    }
  }

  private static ServerHttpResponse getCachingResponseDecorator(ServerHttpResponse response,
      StringBuilder cachedContent) {
    return new ServerHttpResponseDecorator(response) {
      @Override
      public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
        DataBufferFactory bufferFactory = super.bufferFactory();
        return super.writeWith(Flux.from(body)
                                   .map((DataBuffer dataBuffer) -> {
                                     try {
                                       byte[] content = readDataBuffer(dataBuffer);
                                       cachedContent.append(new String(content, StandardCharsets.UTF_8));
                                       return bufferFactory.wrap(content);
                                     } finally {
                                       DataBufferUtils.release(dataBuffer);
                                     }
                                   }));
      }
    };
  }

  private static byte[] readDataBuffer(DataBuffer dataBuffer) {
    if (Objects.nonNull(dataBuffer)) {
      byte[] content = new byte[dataBuffer.readableByteCount()];
      dataBuffer.read(content);
      return content;
    }
    return new byte[]{};
  }
}
