package com.domainname.next.shippingapi.filter;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;


@Component
@RequiredArgsConstructor
@Slf4j
public class MDCManager {

  // MDC constants
  public static final String REQUEST_URL = "request.url";
  public static final String REQUEST_HEADERS = "request.headers";
  public static final String REQUEST_BODY = "request.body";
  public static final String REQUEST_METHOD = "request.method";

  public static final String OUTGOING_REQUEST_HEADERS = "outgoingRequest.request.headers";
  public static final String OUTGOING_REQUEST_BODY = "outgoingRequest.request.body";
  public static final String OUTGOING_REQUEST_METHOD = "outgoingRequest.request.method";
  public static final String OUTGOING_REQUEST_URL = "outgoingRequest.request.url";

  public static final String OUTGOING_RESPONSE_HEADERS = "outgoingRequest.response.headers";
  public static final String OUTGOING_RESPONSE_BODY = "outgoingRequest.response.body";
  public static final String OUTGOING_RESPONSE_STATUS = "outgoingRequest.response.status";

  public static final String REQUEST_SITE_ID = "siteId";
  public static final String REQUEST_CHANNEL = "channel";
  public static final String REQUEST_BASKET_REFERENCE_ID = "basketReferenceId";

  public static final String RESPONSE_STATUS_CODE = "response.statusCode";
  public static final String RESPONSE_BODY = "response.body";
  public static final String RESPONSE_HEADERS = "response.headers";

  @Autowired
  private ObjectMapper mapper;

  @Value("${logging.logBody}")
  boolean isLogBody;

  @Value("${logging.sanitize.headers}")
  private String sanitizedHeaders;
  @Value("${logging.sanitize.query-param}")
  private String sanitizeQueryParam;

  public void insertResponseMDC(ServerWebExchange exchange, StringBuilder cachedResponse) {
    putResponseStatusCode(exchange);
    putResponseHeaders(exchange.getResponse());
    if (isLogBody) {
      putSanitizedResponse(cachedResponse);
    }
  }


  public void insertOutgoingClientRequest (Request clientRequest) {
    MDC.put(OUTGOING_REQUEST_METHOD,
            Optional.of(clientRequest.getMethod())
                .orElse(null));

    MDC.put(OUTGOING_REQUEST_URL,
            Optional.of(clientRequest.getURI())
                .map(uri -> {
                  if (uri.toString().contains(sanitizeQueryParam)) {
                    UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUri(uri);
                    return uriBuilder.replaceQueryParam(sanitizeQueryParam, "****").build().toUri();
                  } else {
                    return uri;
                  }
                })
                .map(URI::toString)
                .orElse(null));
    clientRequest.getHeaders().stream()
        .filter(header -> !sanitizedHeaders.contains(header.getName().toLowerCase()))
        .forEach(httpField -> MDC.put(OUTGOING_REQUEST_HEADERS + "." + httpField.getName(), httpField.getValue()));
  }

  public void insertOutgoingClientResponse (Response response, ByteBuffer buffer) {
    response.getHeaders().stream()
        .filter(header -> !sanitizedHeaders.contains(header.getName().toLowerCase()))
            .forEach(httpHeader -> MDC.put(OUTGOING_RESPONSE_HEADERS + "." + httpHeader.getName(), httpHeader.getValue()));
    MDC.put(OUTGOING_RESPONSE_STATUS, Integer.toString(response.getStatus()));
    if (isLogBody) {
      MDC.put(OUTGOING_RESPONSE_BODY, StandardCharsets.UTF_8.decode(buffer).toString());
    }
  }

  public void cleanupOutgoingRequestFields() {
    MDC.remove(OUTGOING_RESPONSE_BODY);
    MDC.remove(OUTGOING_RESPONSE_HEADERS);
    MDC.remove(OUTGOING_RESPONSE_STATUS);
    MDC.remove(OUTGOING_REQUEST_BODY);
    MDC.remove(OUTGOING_REQUEST_URL);
    MDC.remove(OUTGOING_REQUEST_METHOD);
    MDC.remove(OUTGOING_REQUEST_HEADERS);
  }

  public Map<String, String> getMDCFields() {
    return MDC.getCopyOfContextMap();
  }

  public void insertRequestMDC(ServerWebExchange exchange) {
    putRequestURL(exchange.getRequest());
    putMethod(exchange.getRequest());
    putRequestHeaders(exchange.getRequest());
    putSanitizedRequest(exchange);
  }

  public void clearMDC() {
    MDC.remove(REQUEST_URL);
    MDC.remove(REQUEST_METHOD);
    MDC.remove(REQUEST_HEADERS);
    MDC.remove(REQUEST_BODY);
    MDC.remove(REQUEST_SITE_ID);
    MDC.remove(REQUEST_CHANNEL);
    MDC.remove(REQUEST_BASKET_REFERENCE_ID);
    MDC.remove(RESPONSE_STATUS_CODE);
    MDC.remove(RESPONSE_BODY);
    MDC.remove(RESPONSE_HEADERS);
  }

  private void putSanitizedRequest(ServerWebExchange exchange) {
    String body = sanitizeRequest(exchange);
    LoggingFulfillmentOptionsRequestParameter parameters = null;
    try {
      parameters = mapper.readValue(body, LoggingFulfillmentOptionsRequestParameter.class);
      MDC.put(REQUEST_SITE_ID, parameters.getSiteId());
      MDC.put(REQUEST_CHANNEL, parameters.getChannel());
      MDC.put(REQUEST_BASKET_REFERENCE_ID, parameters.getBasketReferenceId());
      if (isLogBody) {
        MDC.put(REQUEST_BODY, body);
      }
    } catch (JsonProcessingException e) {
      log.warn("Cannot log body ", e);
    }
  }

  private void putSanitizedResponse(StringBuilder cachedResponse) {
    MDC.put(RESPONSE_BODY, cachedResponse.toString());
  }

  private String sanitizeRequest(ServerWebExchange exchange) {
    // Request is cached in attributes by default
    DataBuffer dataBuffer =
        exchange.getAttribute(ServerWebExchangeUtils.CACHED_REQUEST_BODY_ATTR);
    byte[] content = readDataBuffer(dataBuffer);
    return new String(content, StandardCharsets.UTF_8);
  }

  private void putResponseStatusCode(ServerWebExchange exchange) {
    MDC.put(RESPONSE_STATUS_CODE,
            Optional.ofNullable(exchange.getResponse().getRawStatusCode())
                .map(Object::toString)
                .orElse(null));
  }

  private byte[] readDataBuffer(DataBuffer dataBuffer) {
    if (Objects.nonNull(dataBuffer)) {
      byte[] content = new byte[dataBuffer.readableByteCount()];
      dataBuffer.read(content);
      return content;
    }
    return new byte[]{};
  }

  private void putMethod(ServerHttpRequest request) {
    HttpMethod method = request.getMethod();
    if (Objects.nonNull(method)) {
      MDC.put(REQUEST_METHOD, method.name());
    }
  }

  private void putRequestURL(ServerHttpRequest request) {
    MDC.put(REQUEST_URL, request.getURI().toString());
  }

  private void putRequestHeaders (ServerHttpRequest request) {
    HttpHeaders headers = request.getHeaders();
    headers.entrySet().stream().forEach(entry -> {
      try {
        MDC.put(REQUEST_HEADERS + "." + entry.getKey(), mapper.writeValueAsString(entry.getValue()));
      } catch (JsonProcessingException e) {
        log.warn("cannot log headers", e);
      }
    });

  }

  private void putResponseHeaders (ServerHttpResponse response) {
    HttpHeaders headers = response.getHeaders();
    headers.entrySet().stream().forEach(entry -> {
      try {
        MDC.put(RESPONSE_HEADERS + "." + entry.getKey(), mapper.writeValueAsString(entry.getValue()));
      } catch (JsonProcessingException e) {
        log.warn("cannot log headers", e);
      }
    });
  }

}
