package com.domainname.next.shippingapi.controller;

import static com.domainname.next.shippingapi.constant.ErrorConstants.INVALID_FIELD_CODE;
import static com.domainname.next.shippingapi.constant.ErrorConstants.RECORD_NOT_FOUND_CODE_SHIPPING_METHOD_CARRIER_STRING;
import static com.domainname.next.shippingapi.constant.ErrorConstants.SITEID_NOT_FOUND_CODE;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.domainname.next.shippingapi.config.database.ReadOnlyRequestMappingsProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.client.HttpClientErrorException;
import org.zalando.problem.Status;

import com.domainname.next.shippingapi.TestHelper;
import com.domainname.next.shippingapi.exception.NotFoundException;
import com.domainname.next.shippingapi.resources.request.ShippingMethodPostRequest;
import com.domainname.next.shippingapi.resources.request.ShippingPatchRequest;
import com.domainname.next.shippingapi.resources.response.CarrierStringRecord;
import com.domainname.next.shippingapi.service.ShippingCarrierStringService;
import com.domainname.next.shippingapi.service.ShippingService;
import com.domainname.next.shippingapi.util.JsonObjectMapper;
import com.domainname.next.shippingapi.util.MessageHelper;

import io.r2dbc.postgresql.codec.Json;
import reactor.core.publisher.Mono;

@ExtendWith(SpringExtension.class)
@WebFluxTest(controllers = ShippingController.class)
class ShippingControllerTest extends TestHelper {

  @Autowired
  WebTestClient webTestClient;

  @MockBean
  ReadOnlyRequestMappingsProperties readOnlyRequestMappingsProperties;

  @MockBean
  ShippingService shippingService;

  @MockBean
  MessageHelper messageHelper;
  
  @MockBean
  ShippingCarrierStringService shippingCarrierStringService;
  

  @MockBean
  JsonObjectMapper jsonObjectMapper;
  
  @BeforeEach
  void setUp() {
    Json json = Mockito.mock(Json.class);
    Mockito.when(jsonObjectMapper.toJson(Mockito.any())).thenReturn(json);
    Mockito.when(json.asString()).thenReturn("abc");  
  }

  @Test
  void testPostShippingSuccess() {
    Mockito.when(shippingService.createShippingMethods(Mockito.any()))
        .thenReturn(Mono.just(buildShippingMethodsResponseForPost()));
    ShippingMethodPostRequest shippingMethodPostRequest = buildShippingPostRequest();
    webTestClient.post()
        .uri("/shipping-method").bodyValue(shippingMethodPostRequest)
        .header("api-key", "xyz")
        .exchange()
        .expectStatus()
        .isCreated().expectBody()
        .jsonPath("$.shippingMethodId").isEqualTo("760565d9-3e78-4b27-bf6e-b64912e3c531")
        .jsonPath("$.siteId").isEqualTo("domainname-US")
        .jsonPath("$.carrierString").isEqualTo("FED001US0000000000")
        .jsonPath("$.carrierStringRecords[0].carrierString").isEqualTo("UPS100ES0600005700")
        .jsonPath("$.enabled").isEqualTo(Boolean.TRUE)
        .jsonPath("$.position").isEqualTo(1)
        .jsonPath("$.customId").isEqualTo("Standard-FED-Unregistered")
        .jsonPath("$.taxClassId").isEqualTo("classId");
  }


  @Test
  void testPostShippingMethodsWhenDataIntegrity() {
    Mockito.when(shippingService.createShippingMethods(Mockito.any()))
        .thenReturn(Mono.error(new DataIntegrityViolationException("")));
    Mockito.when(messageHelper.buildMessage(Mockito.any(), Mockito.any())).thenReturn("Conflict");
    ShippingMethodPostRequest shippingMethodPostRequest = buildShippingPostRequest();
    webTestClient.post()
    .uri("/shipping-method")
    .bodyValue(shippingMethodPostRequest).exchange().expectStatus()
        .is4xxClientError().expectBody()
        .jsonPath("$.title").isEqualTo(Status.CONFLICT.getReasonPhrase());
  }
  
  @Test
  void testPostShippingMethodsWhenMandatoryFieldMissing() {
    ShippingMethodPostRequest shippingMethodPostRequest = buildShippingPostRequestWithBadRequest();
    webTestClient.post()
    .uri("/shipping-method")
    .bodyValue(shippingMethodPostRequest).exchange().expectStatus()
        .is4xxClientError().expectBody()
        .jsonPath("$.title").isEqualTo(Status.BAD_REQUEST.getReasonPhrase());
  }
  
  @Test
  void testPostShippingMethodsWhenInvalidFieldValue() {
    ShippingMethodPostRequest shippingMethodPostRequest = buildShippingPostRequestWithUnprocessableEntity();
    webTestClient.post()
    .uri("/shipping-method")
    .bodyValue(shippingMethodPostRequest).exchange().expectStatus()
        .is4xxClientError().expectBody()
        .jsonPath("$.title").isEqualTo(Status.UNPROCESSABLE_ENTITY.getReasonPhrase());
  }

  @Test
  void testPostShippingJsonInvalidJson() {
    webTestClient.post()
        .uri("/shipping-method")
        .bodyValue("invalidBody").exchange().expectStatus()
        .is4xxClientError()
        .expectBody()
        .jsonPath("$.title").isEqualTo(Status.UNSUPPORTED_MEDIA_TYPE.getReasonPhrase());
  }
  
  @Test
  void testGetShippingMethodsSuccess() {
    Mockito.when(shippingService.getShippingMethods(Mockito.any()))
        .thenReturn(Mono.just(buildShippingMethodsRuleResponseForGet()));
    webTestClient.get().uri("/shipping-method/domainname-US").exchange().expectStatus()
    .isOk().expectHeader()
        .contentType(MediaType.APPLICATION_JSON_VALUE).expectBody()
        .jsonPath("$[0].siteId").isEqualTo("domainname-US")
        .jsonPath("$[0].fulfillmentTypes.[0]").isEqualTo("HomeDelivery")
        .jsonPath("$[0].enabled").isEqualTo(Boolean.TRUE)
        .jsonPath("$[0].position").isEqualTo(1)
        .jsonPath("$[1].customId").isEqualTo("Standard-FED-Unregistered")
        .jsonPath("$[0].taxClassId").isEqualTo("classId")
        .jsonPath("$[0].carrierStringRecords[0].carrierString").isEqualTo("UPS100ES0600005700");
  }
  
  @Test
  void testGetShippingMethodsInvalidSiteID() {
    Mockito.when(shippingService.getShippingMethods(Mockito.any()))
        .thenReturn(Mono.error(new NotFoundException(new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY),
            INVALID_FIELD_CODE, "domainname-ZX")));
    Mockito.when(messageHelper.buildMessage(Mockito.any(), Mockito.any()))
        .thenReturn("Field value is not valid - getShippingMethods.siteId");
    webTestClient.get().uri("/shipping-method/domainname-ZX").exchange().expectStatus().is4xxClientError()
        .expectBody()
        .jsonPath("$.title").isEqualTo(Status.UNPROCESSABLE_ENTITY.getReasonPhrase())
        .jsonPath("$.detail").isEqualTo("Field value is not valid - getShippingMethods.siteId");
  }
  
  @Test
  void testGetShippingMethodsSiteIDNotFound() {
    Mockito.when(shippingService.getShippingMethods(Mockito.any())).thenReturn(Mono.error(
        new NotFoundException(new HttpClientErrorException(HttpStatus.NOT_FOUND), SITEID_NOT_FOUND_CODE, "domainname-UK")));
    Mockito.when(messageHelper.buildMessage(Mockito.any(), Mockito.any())).thenReturn("SiteID not found - domainname-UK");
    webTestClient.get().uri("/shipping-method/domainname-UK").exchange().expectStatus().is4xxClientError()
        .expectBody()
        .jsonPath("$.title").isEqualTo(Status.NOT_FOUND.getReasonPhrase())
        .jsonPath("$.detail").isEqualTo("SiteID not found - domainname-UK");
  }
  
  @Test
  void testPatchShippingSuccess() {
    Mockito.when(shippingService.updateShippingMethod(Mockito.any(), Mockito.any()))
        .thenReturn(Mono.just(buildShippingMethodsResponseForPost()));
    ShippingPatchRequest shippingPatchRequest = buildShippingPatchRequest();
    webTestClient.patch()
        .uri("/shipping-method/760565d9-3e78-4b27-bf6e-b64912e3c531")
        .bodyValue(shippingPatchRequest)
        .exchange()
        .expectStatus().is2xxSuccessful()        
        .expectBody()
        .jsonPath("$.shippingMethodId").isEqualTo("760565d9-3e78-4b27-bf6e-b64912e3c531")
        .jsonPath("$.fulfillmentTypes").isEqualTo("HomeDelivery")
        .jsonPath("$.enabled").isEqualTo(true)
        .jsonPath("$.default").isEqualTo(true)
        .jsonPath("$.carrierName").isEqualTo("UPS")
        .jsonPath("$.carrierString").isEqualTo("FED001US0000000000")
        .jsonPath("$.customId").isEqualTo("Standard-FED-Unregistered")
        .jsonPath("$.carrierService").isEqualTo("Standard");
  }
  
  @Test
  void testPatchShippingFailure() {
    Mockito.when(shippingService.updateShippingMethod(Mockito.any(), Mockito.any()))
        .thenReturn(Mono.error(new DataAccessResourceFailureException("")));
    Mockito.when(messageHelper.buildMessage(Mockito.any())).thenReturn("Database connection error");
    ShippingPatchRequest shippingPatchRequest = buildShippingPatchRequest();
    webTestClient.patch()
        .uri("/shipping-method/760565d9-3e78-4b27-bf6e-b64912e3c531")
        .bodyValue(shippingPatchRequest)
        .exchange()
        .expectStatus().is5xxServerError()
        .expectBody()
        .jsonPath("$.status").isNotEmpty()
        .jsonPath("$.status").isEqualTo(Status.INTERNAL_SERVER_ERROR.name())
        .jsonPath("$.detail").isNotEmpty()
        .jsonPath("$.detail").isEqualTo("Database connection error");
  }
  
  @Test
  void testPatchShippingFailureInvalidUUID() {
    ShippingPatchRequest shippingPatchRequest = buildShippingPatchRequest();
    webTestClient.patch()
        .uri("/shipping-method/abcd")
        .bodyValue(shippingPatchRequest)
        .exchange()
        .expectStatus().is4xxClientError()
        .expectBody()
        .jsonPath("$.status").isNotEmpty()
        .jsonPath("$.status").isEqualTo(Status.UNPROCESSABLE_ENTITY.name());
  }

  @Test
  void testPatchShippingMissingReqBody() {
    Mockito.when(messageHelper.buildMessage(Mockito.any())).thenReturn("Request Body is Missing");
    webTestClient.patch().uri("/shipping-method/760565d9-3e78-4b27-bf6e-b64912e3c531")
        .exchange()
        .expectStatus().is4xxClientError()
        .expectBody()
        .jsonPath("$.status").isNotEmpty();
  }
  
  @Test
  void testPatchShippingWithMissingCarrierStringRecords() {
    ShippingPatchRequest shippingPatchRequest = buildShippingPatchRequest().toBuilder()
        .carrierStringRecords(Collections.<CarrierStringRecord>emptyList()).build();
    webTestClient.patch().uri("/shipping-method/760565d9-3e78-4b27-bf6e-b64912e3c531")
        .bodyValue(shippingPatchRequest)
        .exchange()
        .expectStatus()
        .is4xxClientError()
        .expectBody()
        .jsonPath("$.title")
        .isEqualTo(Status.UNPROCESSABLE_ENTITY.getReasonPhrase());
  }
  
  @Test
  void testPatchShippingWithNullInCarrierStringRecords() {
    List<CarrierStringRecord> s = Arrays.asList(CarrierStringRecord.builder()
        .carrierStringId("f5ffb268-5ae6-423e-8e88-58f4af0354c1")
        .carrierString("UPS100ES0600005700").build(), null);
    ShippingPatchRequest shippingPatchRequest = buildShippingPatchRequest().toBuilder().carrierStringRecords(s).build();
    webTestClient.patch().uri("/shipping-method/760565d9-3e78-4b27-bf6e-b64912e3c531")
    .bodyValue(shippingPatchRequest)
    .exchange()
    .expectStatus()
    .is4xxClientError()
    .expectBody()
    .jsonPath("$.title")
    .isEqualTo(Status.UNPROCESSABLE_ENTITY.getReasonPhrase());
  }

  @Test
  void testDeleteShippingMethodCarrierString() {
    Mockito.when(shippingCarrierStringService.deleteShippingMethodCarrierString(Mockito.any(), Mockito.any())).thenReturn(Mono.empty());
    webTestClient.delete()
        .uri("/shipping-method/9dba27ae-1225-4601-abdb-181e741c116d/carrierString/fac26cf3-a255-4bec-8134-9660d5124b01")
        .exchange()
        .expectStatus()
        .isNoContent()
        .expectHeader()
        .contentLength(0);
  }

  @Test
  void testDeleteShippingMethodCarrierStringWithInvalidUUID() {
    Mockito.when(shippingCarrierStringService.deleteShippingMethodCarrierString(Mockito.any(), Mockito.any())).thenReturn(Mono.empty());
    webTestClient.delete()
        .uri("/shipping-method/9dba27ae-1225-4601-abdb-181e741c/carrierString/fac26cf3-a255-4bec-8134-9660d5124b0100000")
        .exchange()
        .expectStatus()
        .is4xxClientError().expectBody()
        .jsonPath("$.status").isNotEmpty()
        .jsonPath("$.status").isEqualTo(Status.UNPROCESSABLE_ENTITY.name());
  }
  
  @Test
  void testDeleteShippingMethodsCarrierStringNotFound() {
    Mockito.when(shippingCarrierStringService.deleteShippingMethodCarrierString(Mockito.any(), Mockito.any())).thenReturn(Mono.error(
        new NotFoundException(new HttpClientErrorException(HttpStatus.NOT_FOUND), RECORD_NOT_FOUND_CODE_SHIPPING_METHOD_CARRIER_STRING, "domainname-UK")));
    Mockito.when(messageHelper.buildMessage(Mockito.any(), Mockito.any())).thenReturn("Record not found for carrierStringId- fac26cf3-a255-4bec-8134-9660d5124b01");
    webTestClient.delete().uri("/shipping-method/9dba27ae-1225-4601-abdb-181e741c116d/carrierString/fac26cf3-a255-4bec-8134-9660d5124b01")
    .exchange().expectStatus().is4xxClientError()
        .expectBody()
        .jsonPath("$.title").isEqualTo(Status.NOT_FOUND.getReasonPhrase())
        .jsonPath("$.detail").isEqualTo("Record not found for carrierStringId- fac26cf3-a255-4bec-8134-9660d5124b01");
  }
  
}
