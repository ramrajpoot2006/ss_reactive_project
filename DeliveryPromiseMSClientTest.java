package com.domainname.next.shippingapi.client;

import java.net.URI;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import com.domainname.next.shippingapi.TestHelper;
import com.domainname.next.shippingapi.client.request.DeliveryPromiseRequest;
import com.domainname.next.shippingapi.client.response.DeliveryPromiseResponse;
import com.domainname.next.shippingapi.enums.TargetService;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(SpringExtension.class)
public class DeliveryPromiseMSClientTest extends TestHelper {
  
  @Mock
  DeliveryPromiseRequest dpeRequest;
    
  @InjectMocks
  DeliveryPromiseMSClient dpeMSClient;

  private final WebClient webClient = Mockito.mock(WebClient.class);
  private final WebClient.RequestBodyUriSpec requestBodyUriSpec = Mockito.mock(WebClient.RequestBodyUriSpec.class);
  private final WebClient.RequestBodySpec requestBodySpec = Mockito.mock(WebClient.RequestBodySpec.class);
  @SuppressWarnings("rawtypes")
  private final WebClient.RequestHeadersSpec requestHeadersSpec = Mockito.mock(WebClient.RequestHeadersSpec.class);
  private final WebClient.ResponseSpec responseSpec = Mockito.mock(WebClient.ResponseSpec.class);

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setup() {
    ReflectionTestUtils.setField(dpeMSClient, "host", "https://dpeMSHost");
    ReflectionTestUtils.setField(dpeMSClient, "uri", "dpeMSUri");
    ReflectionTestUtils.setField(dpeMSClient, "xApiKey", "apiKey");

    dpeRequest = buildDPERequest();

    Mockito.when(webClient.post()).thenReturn(requestBodyUriSpec);
    Mockito.when(requestBodyUriSpec.uri(Mockito.any(URI.class))).thenReturn(requestBodySpec);
    Mockito.when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
    Mockito.when(requestBodySpec.headers(Mockito.any())).thenReturn(requestBodySpec);
    Mockito.when(requestBodySpec.body(Mockito.any(), Mockito.eq(DeliveryPromiseRequest.class))).thenReturn(requestHeadersSpec);
    Mockito.when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    Mockito.when(requestBodySpec.header(Mockito.any(),Mockito.any())).thenReturn(requestBodySpec);
    Mockito.when(requestHeadersSpec.header("x-api-key", "apiKey")).thenReturn(requestHeadersSpec);
    Mockito.when(responseSpec.onStatus(Mockito.any(), Mockito.any())).thenReturn(responseSpec);
  }

  @Test
  void testDeliveryPromiseMSSuccessFlow() {
    DeliveryPromiseResponse dpeResponse = buildDPEResponse();
    Mockito.when(responseSpec.bodyToMono(DeliveryPromiseResponse.class)).thenReturn(Mono.just(dpeResponse));

    StepVerifier
        .create(dpeMSClient.getDeliveryPromise(dpeRequest))
        .thenConsumeWhile(dpeRes -> {
          Assertions.assertEquals("16-12-2021T10:00:00.000",
              dpeRes.getShipments().get(0).getCarrierOptions().getCarrierOption().get(0).getPromiseDeliveryDateStart());
          Assertions.assertEquals("16-12-2021T18:00:00.000",
              dpeRes.getShipments().get(0).getCarrierOptions().getCarrierOption().get(0).getPromiseDeliveryDateEnd());
          return true;
        }).verifyComplete();
  }

  @Test
  void testDeliveryPromiseMSFailureFlow() {
    Mockito.when(responseSpec.bodyToMono(DeliveryPromiseResponse.class)).thenReturn(Mono.error(new Exception()));
    StepVerifier.create(dpeMSClient.getDeliveryPromise(dpeRequest))
        .expectErrorMatches(throwable -> throwable instanceof Exception)
        .verify();
  }
  
  @Test
  void testDeliveryPromiseMSEnabled() {
    Map<String, Boolean> targetEnabledMapMock = Map.of(TargetService.DPEV2_HD_MS.getValue(), true);
    Boolean result = dpeMSClient.shouldApply(targetEnabledMapMock);
    Assertions.assertEquals(true, result);
  }

  @Test
  void testDeliveryPromiseMSDisabled() {
    Map<String, Boolean> targetEnabledMapMock = Map.of(TargetService.DPEV2_HD_MS.getValue(), false);
    Boolean result = dpeMSClient.shouldApply(targetEnabledMapMock);
    Assertions.assertEquals(false, result);
  }
}
