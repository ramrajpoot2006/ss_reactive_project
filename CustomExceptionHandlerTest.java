package com.domainname.next.shippingapi.Exception;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.server.ServerWebExchange;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;
import org.zalando.problem.violations.Violation;

import com.domainname.next.shippingapi.constant.ErrorConstants;
import com.domainname.next.shippingapi.exception.CarrierStringsValidationException;
import com.domainname.next.shippingapi.exception.CustomExceptionHandler;
import com.domainname.next.shippingapi.exception.ValidationException;
import com.domainname.next.shippingapi.util.MessageHelper;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(SpringExtension.class)
class CustomExceptionHandlerTest {

  private CustomExceptionHandler customExceptionHandler;
  private MessageHelper messageHelper;
  private Exception ex;
  private ServerWebExchange request;
  private ServerHttpRequest httpRequest;
  private RequestPath requesPath;
  private HttpHeaders headers;
  private ValidationException validationException;
  private CarrierStringsValidationException carrierStringsValidationException;

  @BeforeEach
  public void setUp() {
    messageHelper = Mockito.mock(MessageHelper.class);
    customExceptionHandler = new CustomExceptionHandler(messageHelper);
    ex = Mockito.mock(Exception.class);
    request = Mockito.mock(ServerWebExchange.class);
    httpRequest = Mockito.mock(ServerHttpRequest.class);
    requesPath = Mockito.mock(RequestPath.class);
    headers = Mockito.mock(HttpHeaders.class);
    validationException = Mockito.mock(ValidationException.class);
    carrierStringsValidationException = Mockito.mock(CarrierStringsValidationException.class);
  }

  @Test
  void testHandleCustomException() throws URISyntaxException {
    String errorMsg = "Error occurred while saving Fulfillment Methods";
    String path = "/site-fulfillment-methods";
    Mockito.when(messageHelper.buildMessage("4091")).thenReturn(errorMsg);
    Mockito.when(request.getRequest()).thenReturn(httpRequest);
    Mockito.when(httpRequest.getPath()).thenReturn(requesPath);
    Mockito.when(requesPath.value()).thenReturn(path);

    Mono<ResponseEntity<Problem>> response = customExceptionHandler.handleConflict(ex, request);

    StepVerifier.create(response).thenConsumeWhile(problemResponse -> {
      Assertions.assertNotNull(problemResponse.getBody());
      Assertions.assertEquals(409, problemResponse.getStatusCodeValue());
      Assertions.assertEquals("Conflict", problemResponse.getBody().getTitle());
      Assertions.assertEquals("Error occurred while saving Fulfillment Methods", problemResponse.getBody().getDetail());
      return true;
    }).verifyComplete();

  }
  
  @Test
  void testCreateRequestBodyIsMissing() throws NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
    Mockito.when(messageHelper.buildMessage("4002")).thenReturn("Request Body is Missing");
    String message = "400 BAD_REQUEST Request body is missing: public reactor.core.publisher.Mono<org.springframework.http.ResponseEntity<java.util.List<com.domainname.next.shippingapi.resources.response.FulfillmentMethodsResponse>>> com.domainname.next.shippingapi.controller.FulfillmentController.createFulfillmentMethods(java.util.List<com.domainname.next.shippingapi.resources.request.FulfillmentMethodPostRequest>)";
    Throwable throwable = createWithCause(RuntimeException.class, message, new Throwable());
    
    Mono<ResponseEntity<Problem>> response = customExceptionHandler.create(Status.BAD_REQUEST, throwable, request, headers);

    StepVerifier.create(response).thenConsumeWhile(problemResponse -> {
      Assertions.assertNotNull(problemResponse.getBody());
      Assertions.assertEquals(400, problemResponse.getStatusCodeValue());
      Assertions.assertEquals("Bad Request", problemResponse.getBody().getTitle());
      Assertions.assertEquals("Request Body is Missing", problemResponse.getBody().getDetail());
      return true;
    }).verifyComplete();
  }
  
  @Test
  void testCreateRequestInvalidJson() throws NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
    Mockito.when(messageHelper.buildMessage("4003")).thenReturn("Request Body is Missing");
    String message = "org.springframework.web.server.ServerWebInputException: 400 BAD_REQUEST \"Failed to read HTTP message\"; nested exception is org.springframework.core.codec.DecodingException: JSON decoding error: Unexpected character ('\"' (code 34)): was expecting comma to separate Object entries; nested exception is com.fasterxml.jackson.databind.JsonMappingException: Unexpected character ('\"' (code 34)): was expecting comma to separate Object entries at [Source: (io.netty.buffer.ByteBufInputStream); line: 5, column: 10] (through reference chain: java.util.ArrayList[0])";
    Throwable throwable = createWithCause(RuntimeException.class, message, new Throwable());
    
    Mono<ResponseEntity<Problem>> response = customExceptionHandler.create(Status.BAD_REQUEST, throwable, request, headers);

    StepVerifier.create(response).thenConsumeWhile(problemResponse -> {
      Assertions.assertNotNull(problemResponse.getBody());
      Assertions.assertEquals(400, problemResponse.getStatusCodeValue());
      Assertions.assertEquals("Bad Request", problemResponse.getBody().getTitle());
      Assertions.assertEquals("Request Body is Missing", problemResponse.getBody().getDetail());
      return true;
    }).verifyComplete();
  }
  
  private Throwable createWithCause(Class<RuntimeException> clazz, String message, Throwable cause)
      throws NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException,
      IllegalArgumentException, InvocationTargetException {
    Throwable re = null;
   Constructor<RuntimeException> constructor = clazz.getConstructor(new Class[] { String.class, Throwable.class });
   re = (Throwable) constructor.newInstance(new Object[] { message, cause });
    return re;
  }
  @Test
  void testNewConstraintViolationWithBadRequest() {
  MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(""));
  Violation v = new Violation("siteId", ErrorConstants.INTERNAL_ERROR_CODE);
  List<Violation> violations = new ArrayList<>();
  violations.add(v);
  Mono<ResponseEntity<Problem>> response = customExceptionHandler
  .newConstraintViolationProblem(new InternalError("Internal Server Error"), violations, exchange);
  StepVerifier.create(response).thenConsumeWhile(problemResponse -> {
  Assertions.assertNotNull(problemResponse.getBody());
  Assertions.assertEquals(400, problemResponse.getStatusCodeValue());
  return true;
  }).verifyComplete();
  }
  
  @Test
  void testNewConstraintViolation() {
  MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(""));
  Mono<ResponseEntity<Problem>> response = customExceptionHandler.newConstraintViolationProblem(
  new InternalError("Internal Server Error"), new ArrayList<Violation>(), exchange);
  StepVerifier.create(response).thenConsumeWhile(problemResponse -> {
  Assertions.assertNotNull(problemResponse.getBody());
  Assertions.assertEquals(500, problemResponse.getStatusCodeValue());
  Assertions.assertEquals("Internal Server Error", problemResponse.getBody().getTitle());
  return true;
  }).verifyComplete();
  }

  @Test
  void testHandleValidationException() throws URISyntaxException {
    String errorMsg = "Error occurred while saving Fulfillment Methods";
    String path = "/site-fulfillment-methods";
    Mockito.when(messageHelper.buildMessage("422")).thenReturn(errorMsg);
    Mockito.when(request.getRequest()).thenReturn(httpRequest);
    Mockito.when(httpRequest.getPath()).thenReturn(requesPath);
    Mockito.when(requesPath.value()).thenReturn(path);
    Mono<ResponseEntity<Problem>> response = customExceptionHandler.handleValidationException(validationException, request);
    StepVerifier.create(response).thenConsumeWhile(problemResponse -> {
      Assertions.assertNotNull(problemResponse.getBody());
      Assertions.assertEquals(422, problemResponse.getStatusCodeValue());
      Assertions.assertEquals("Unprocessable Entity", problemResponse.getBody().getTitle());
      return true;
    }).verifyComplete();
  }
  
  @Test
  void testHandleCarrierStringValidationException() throws URISyntaxException {
    String errorMsg = "Error occurred while saving Fulfillment Methods";
    String path = "/site-fulfillment-methods";
    Mockito.when(messageHelper.buildMessage("409")).thenReturn(errorMsg);
    Mockito.when(request.getRequest()).thenReturn(httpRequest);
    Mockito.when(httpRequest.getPath()).thenReturn(requesPath);
    Mockito.when(requesPath.value()).thenReturn(path);
    Mono<ResponseEntity<Problem>> response = customExceptionHandler.handleCarrierStringValidationException(carrierStringsValidationException, request);
    StepVerifier.create(response).thenConsumeWhile(problemResponse -> {
      Assertions.assertNotNull(problemResponse.getBody());
      Assertions.assertEquals(409, problemResponse.getStatusCodeValue());
      Assertions.assertEquals("Conflict", problemResponse.getBody().getTitle());
      return true;
    }).verifyComplete();

  }
}