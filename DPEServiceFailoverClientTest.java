package com.domainname.next.shippingapi.client;

import static org.mockito.ArgumentMatchers.any;

import java.net.URI;
import java.util.function.Function;
import java.util.function.Predicate;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.domainname.next.shippingapi.TestHelper;
import com.domainname.next.shippingapi.client.request.DeliveryPromiseRequest;
import com.domainname.next.shippingapi.client.response.DeliveryPromiseResponse;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(SpringExtension.class)
class DPEServiceFailoverClientTest extends TestHelper {

  @InjectMocks
  DPEFailoverClient dpeServiceFailoverClient;
  
  @Mock
  DeliveryPromiseRequest dpeRequest;

  private final WebClient webClient = Mockito.mock(WebClient.class);
  private final WebClient.RequestBodyUriSpec requestBodyUriSpec = Mockito.mock(WebClient.RequestBodyUriSpec.class);
  private final WebClient.RequestBodySpec requestBodySpec = Mockito.mock(WebClient.RequestBodySpec.class);
  @SuppressWarnings("rawtypes")
  private final WebClient.RequestHeadersSpec requestHeadersSpec = Mockito.mock(WebClient.RequestHeadersSpec.class);
  private final WebClient.ResponseSpec responseSpec = Mockito.mock(WebClient.ResponseSpec.class);
  private final ClientResponse clientResponse =  Mockito.mock(ClientResponse.class);
  private final WebClientResponseException webClientException = Mockito.mock(WebClientResponseException.class);
  
  @BeforeEach
  void setup() {
    ReflectionTestUtils.setField(dpeServiceFailoverClient, "uri", "deliveryPromise/V2");
    ReflectionTestUtils.setField(dpeServiceFailoverClient, "appId", "xyz");
    ReflectionTestUtils.setField(dpeServiceFailoverClient, "host", "https://dev-omnihub.api.3stripes.io");
    dpeRequest = buildDPERequest();
  }

  @Test
  @SuppressWarnings("unchecked")
  void testDeliveryPromiseFailoverInfoSuccessFlow() {
    DeliveryPromiseResponse dpeResponse = buildDPEResponse();
    Mockito.when(webClient.post()).thenReturn(requestBodyUriSpec);
    Mockito.when(requestBodyUriSpec.uri(Mockito.any(URI.class))).thenReturn(requestBodySpec);
    Mockito.when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
    Mockito.when(requestBodySpec.headers(Mockito.any())).thenReturn(requestBodySpec);
    Mockito.when(requestBodySpec.body(Mockito.any(), Mockito.eq(DeliveryPromiseRequest.class))).thenReturn(requestHeadersSpec);
    Mockito.when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    Mockito.when(requestBodySpec.header(Mockito.any(),Mockito.any())).thenReturn(requestBodySpec);
    Mockito.when(requestHeadersSpec.header(Mockito.anyString(),Mockito.anyString())).thenReturn(requestHeadersSpec);
    Mockito.when(responseSpec.onStatus(Mockito.any(), Mockito.any())).thenReturn(responseSpec);
    Mockito.when(responseSpec.bodyToMono(DeliveryPromiseResponse.class)).thenReturn(Mono.just(dpeResponse));
    StepVerifier
        .create(dpeServiceFailoverClient.getDeliveryPromiseFailover(dpeRequest))
        .thenConsumeWhile(dpeRes -> {
          Assertions.assertEquals("16-12-2021T10:00:00.000", dpeRes.getShipments().get(0).getCarrierOptions().getCarrierOption().get(0).getPromiseDeliveryDateStart());
          Assertions.assertEquals("16-12-2021T18:00:00.000", dpeRes.getShipments().get(0).getCarrierOptions().getCarrierOption().get(0).getPromiseDeliveryDateEnd());
          return true;
        }).verifyComplete();
  }

  @Test
  @SuppressWarnings("unchecked")
  void testDeliveryPromiseFailoverInfoFailureFlow() {
    Mockito.when(clientResponse.statusCode()).thenReturn(HttpStatus.NOT_FOUND);
    Mockito.when(clientResponse.createException()).thenReturn(Mono.just(webClientException));
    Mockito.doAnswer(invocation -> {
      HttpStatus httpStatus = HttpStatus.NOT_FOUND;
      Predicate<HttpStatus> predicate = invocation.getArgument(0);
      Function<ClientResponse, Mono<? extends Throwable>> function = invocation.getArgument(1);
      predicate.test(httpStatus);
      function.apply(clientResponse);
      return responseSpec;
    }).when(responseSpec).onStatus(any(Predicate.class), any(Function.class));
    Mockito.when(webClient.post()).thenReturn(requestBodyUriSpec);
    Mockito.when(requestBodyUriSpec.uri(Mockito.any(URI.class))).thenReturn(requestBodySpec);
    Mockito.when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
    Mockito.when(requestBodySpec.headers(Mockito.any())).thenReturn(requestBodySpec);
    Mockito.when(requestBodySpec.body(Mockito.any(Mono.class), Mockito.eq(DeliveryPromiseRequest.class))).thenReturn(requestHeadersSpec);
    Mockito.when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    Mockito.when(requestBodySpec.header(Mockito.any(),Mockito.any())).thenReturn(requestBodySpec);
    Mockito.when(requestHeadersSpec.header(Mockito.anyString(),Mockito.anyString())).thenReturn(requestHeadersSpec);
    Mockito.when(responseSpec.bodyToMono(DeliveryPromiseResponse.class)).thenReturn(Mono.error(new Exception()));
    StepVerifier.create(dpeServiceFailoverClient.getDeliveryPromiseFailover(dpeRequest))
        .expectErrorMatches(throwable -> throwable instanceof Exception)
        .verify();
  }
}
