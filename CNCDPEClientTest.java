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
import com.domainname.next.shippingapi.client.request.CNCMSRequest;
import com.domainname.next.shippingapi.client.response.cnc.CNCMSResponse;
import com.domainname.next.shippingapi.client.response.cnc.ExpectedPickupDate;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(SpringExtension.class)
public class CNCDPEClientTest extends TestHelper {
  
  @InjectMocks
  CNCMSClient cncDPEClient;
  
  @Mock
  CNCMSRequest cncDPERequest;
  
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
    ReflectionTestUtils.setField(cncDPEClient, "uri", "/clickandcollect/options/v3");
    ReflectionTestUtils.setField(cncDPEClient, "xApiKey", "test");
    ReflectionTestUtils.setField(cncDPEClient, "host", "https://stg-omnideliverypromise.api.3stripes.io");
    cncDPERequest = buildCNCMSRequest();

  }
  
  @SuppressWarnings("unchecked")
  @Test
  void testCNCDPEInfoSuccessFlow() {
    CNCMSResponse cNCDPEResponse = buildCNCMSResponse("Y");
    Mockito.when(webClient.post()).thenReturn(requestBodyUriSpec);
    Mockito.when(requestBodyUriSpec.uri(Mockito.any(URI.class))).thenReturn(requestBodySpec);
    Mockito.when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
    Mockito.when(requestBodySpec.accept(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
    Mockito.when(requestBodySpec.header(Mockito.anyString(), Mockito.anyString())).thenReturn(requestBodySpec);
    Mockito.when(requestBodySpec.body(Mockito.any(), Mockito.eq(CNCMSRequest.class))).thenReturn(requestHeadersSpec);
    Mockito.when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    Mockito.when(responseSpec.onStatus(Mockito.any(), Mockito.any())).thenReturn(responseSpec);
    Mockito.when(responseSpec.bodyToMono(CNCMSResponse.class)).thenReturn(Mono.just(cNCDPEResponse));
    StepVerifier
        .create(cncDPEClient.getCNCDPE(cncDPERequest, "web", "domainnameUS"))
        .thenConsumeWhile(clickAndCollectDPERes -> {
          Assertions.assertEquals(cNCDPEResponse.getReferenceId(), clickAndCollectDPERes.getReferenceId());
          Assertions.assertEquals(cNCDPEResponse.getNodeList().getNode().get(0).getShipNode(), 
              clickAndCollectDPERes.getNodeList().getNode().get(0).getShipNode());
          
          ExpectedPickupDate expectedPickupDate = cNCDPEResponse.getNodeList().getNode().get(0)
              .getExpectedPickupDates().getExpectedPickupDate().get(0);
          Assertions.assertEquals(
              expectedPickupDate.getPromiseLines().getPromiseLine().get(0).getItemID(),
              clickAndCollectDPERes.getNodeList().getNode().get(0).getExpectedPickupDates()
                  .getExpectedPickupDate().get(0).getPromiseLines().getPromiseLine().get(0)
                  .getItemID());

          Assertions.assertEquals(expectedPickupDate.getExpectedPickupDate(),
              clickAndCollectDPERes.getNodeList().getNode().get(0).getExpectedPickupDates()
                  .getExpectedPickupDate().get(0).getExpectedPickupDate());

          Assertions.assertEquals(
              cNCDPEResponse.getNodeList().getNode().get(0).getExpectedPickupDates()
                  .getExpectedPickupDate().get(0).getAvailable(),
              clickAndCollectDPERes.getNodeList().getNode().get(0).getExpectedPickupDates()
                  .getExpectedPickupDate().get(0).getAvailable());
          return true;
        }).verifyComplete();
  }
  
  @SuppressWarnings("unchecked")
  @Test
  void testCNCDPEInfoFailureFlow() {
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
    Mockito.when(requestBodySpec.accept(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
    Mockito.when(requestBodySpec.header(Mockito.anyString(), Mockito.anyString())).thenReturn(requestBodySpec);
    Mockito.when(requestBodySpec.body(Mockito.any(Mono.class), Mockito.eq(CNCMSRequest.class))).thenReturn(requestHeadersSpec);
    Mockito.when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    Mockito.when(responseSpec.bodyToMono(CNCMSResponse.class)).thenReturn(Mono.error(new Exception()));
    StepVerifier.create(cncDPEClient.getCNCDPE(cncDPERequest, "web", "domainnameUS"))
    .expectErrorMatches(throwable -> throwable instanceof Exception)
    .verify();
  }
}
