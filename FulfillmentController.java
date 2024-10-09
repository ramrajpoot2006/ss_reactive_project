package com.domainname.next.shippingapi.controller;

import static com.domainname.next.shippingapi.constant.ErrorConstants.INVALID_FIELD_CODE;
import static com.domainname.next.shippingapi.constant.ErrorConstants.REQUIRED_FIELD_CODE;

import java.util.List;
import java.util.UUID;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.domainname.next.shippingapi.annotation.EnumPattern;
import com.domainname.next.shippingapi.constant.PatternConstants;
import com.domainname.next.shippingapi.enums.SiteID;
import com.domainname.next.shippingapi.resources.request.FulfillmentMethodPostRequest;
import com.domainname.next.shippingapi.resources.request.FulfillmentPatchRequest;
import com.domainname.next.shippingapi.resources.response.FulfillmentMethodsResponse;
import com.domainname.next.shippingapi.service.FulfillmentService;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/fulfillment-method")
@Slf4j
@Validated
public class FulfillmentController {

  private final FulfillmentService fulfillmentService;

  public FulfillmentController(FulfillmentService fulfillmentService) {
    this.fulfillmentService = fulfillmentService;
  }

  @GetMapping(value = "/{siteId}", produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<List<FulfillmentMethodsResponse>>> getFulfillmentMethods(
      @PathVariable @NotBlank(message = REQUIRED_FIELD_CODE) @EnumPattern(targetClassType = SiteID.class, message = INVALID_FIELD_CODE) String siteId) {
         return fulfillmentService.getFulfillmentMethods(siteId)
        .doFirst(() -> log.info("Request received to get fulfillment methods for siteId : {}", siteId))
        .map(fulfillmentMethods -> ResponseEntity.ok().body(fulfillmentMethods))
        .doOnSuccess(id -> log.info("Fetched fulfillment methods for siteId successfully : {}", siteId))
        .doOnError(throwable -> log.error("Error occurred for siteId {} : {}", siteId, throwable.getMessage()));
  }

  @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<FulfillmentMethodsResponse>> createFulfillmentMethods(
      @Valid @RequestBody FulfillmentMethodPostRequest fulfillmentRequest) {
    String siteId = fulfillmentRequest.getSiteId();
    return fulfillmentService.createFulfillmentMethods(fulfillmentRequest)
        .doFirst(() -> log.info("Request received to create fulfillment method {}",siteId))
        .map(fulfillmentMethods -> ResponseEntity.status(HttpStatus.CREATED).body(fulfillmentMethods))
        .doOnSuccess(request -> log.info("Fulfillment method created successfully : {}",siteId))
        .doOnError(throwable -> log.error("Error in saving fulfillment methods : {}", siteId));
  }
  
  @PatchMapping(value = "/{fulfillmentId}", produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<FulfillmentMethodsResponse>> updateFulfillmentMethod(
      @PathVariable @NotBlank(message = REQUIRED_FIELD_CODE) @Pattern(regexp = PatternConstants.UUID_PATTERN, message = INVALID_FIELD_CODE) String fulfillmentId,
      @Valid @RequestBody FulfillmentPatchRequest fulfillmentPatchRequest) {
    return fulfillmentService.updateFulfillmentMethod(fulfillmentPatchRequest, UUID.fromString(fulfillmentId))
        .doFirst(() -> log.info("Request received to update fulfillment method for fulfillmentId {}",
             fulfillmentId))
        .map(fulfillment -> ResponseEntity.ok().body(fulfillment))
        .doOnSuccess(id -> log.info(
            "Fulfillment method updated successfully fulfillmentId : {}",fulfillmentId))
        .doOnError(throwable -> log.error("Error occurred for fulfillmentId : {}", fulfillmentId,
            throwable.getMessage()));
  }

}
