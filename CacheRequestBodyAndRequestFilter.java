package com.domainname.next.shippingapi.filter;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.CACHED_REQUEST_BODY_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.CACHED_SERVER_HTTP_REQUEST_DECORATOR_ATTR;

import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

/**
 * Filter which enhances the exchange by caching the request and releasing it after execution of the chain.
 *
 * It is required for logging, and maybe other later utilities, where it is necessary to read the request body multiple times.
 */
@Component
public class CacheRequestBodyAndRequestFilter implements WebFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return ServerWebExchangeUtils.cacheRequestBodyAndRequest(exchange, serverHttpRequest -> {
            final ServerRequest serverRequest = ServerRequest
                .create(exchange.mutate().request(serverHttpRequest).build(), HandlerStrategies.withDefaults().messageReaders());
            return serverRequest.bodyToMono((DataBuffer.class)).doOnNext(objectValue ->
                exchange.getAttributes().put(ServerWebExchangeUtils.CACHED_REQUEST_BODY_ATTR, objectValue)
            ).then(Mono.defer(() -> {
                ServerHttpRequest cachedRequest = exchange
                    .getAttribute(CACHED_SERVER_HTTP_REQUEST_DECORATOR_ATTR);
                exchange.getAttributes().remove(CACHED_SERVER_HTTP_REQUEST_DECORATOR_ATTR);
                return chain.filter(exchange.mutate().request(cachedRequest).build());
            })).doFinally(s -> {
                Object attribute = exchange.getAttributes().remove(CACHED_REQUEST_BODY_ATTR);
                if (attribute instanceof DataBuffer) {
                    DataBuffer dataBuffer = (DataBuffer) attribute;
                    DataBufferUtils.release(dataBuffer);
                }
            });
        });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

}
