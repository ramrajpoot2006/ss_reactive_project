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
import org.springframework.web.bind.annotation.DeleteMapping;
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
import com.domainname.next.shippingapi.resources.request.ShippingMethodPostRequest;
import com.domainname.next.shippingapi.resources.request.ShippingPatchRequest;
import com.domainname.next.shippingapi.resources.response.ShippingMethodsResponse;

import com.domainname.next.shippingapi.resources.response.rule.ShippingMethodsRuleResponse;
import com.domainname.next.shippingapi.service.ShippingCarrierStringService;
import com.domainname.next.shippingapi.service.ShippingService;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/shipping-method")
@Slf4j
@Validated
public class ShippingController {

  private final ShippingService shippingService;
  private final ShippingCarrierStringService shippingCarrierStringService;
  

  public ShippingController(ShippingService shippingService, ShippingCarrierStringService shippingCarrierStringService) {
    this.shippingService = shippingService;
    this.shippingCarrierStringService = shippingCarrierStringService;
  }

  @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<ShippingMethodsResponse>> createShippingMethods(
      @Valid @RequestBody ShippingMethodPostRequest shippingRequest) {
    String siteId = shippingRequest.getSiteId();
    return shippingService.createShippingMethods(shippingRequest)
        .doFirst(() -> log.info("Request received to create shipping methods {}", siteId))
        .map(shippingMethods -> ResponseEntity.status(HttpStatus.CREATED).body(shippingMethods))
        .doOnSuccess(request -> log.info("Shipping methods created successfully : {}", siteId))
        .doOnError(throwable -> log.error("Error in saving shipping methods : {} : {}", siteId, throwable.getMessage()));
  }
  
  @GetMapping(value = "/{siteId}", produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<List<ShippingMethodsRuleResponse>>> getShippingMethods(
      @PathVariable @NotBlank(message = REQUIRED_FIELD_CODE) @EnumPattern(targetClassType = SiteID.class, message = INVALID_FIELD_CODE) String siteId) {
    log.info("Request received to get shipping methods for siteId : {}", siteId);
    return shippingService.getShippingMethods(siteId)
        .map(shippingMethods -> ResponseEntity.ok().body(shippingMethods))
        .doOnEach(id -> log.info("Fetched shipping methods for siteId successfully : {}", siteId))
        .doOnError(throwable -> log.error("Error occurred while getting shipping methods for siteId {} : {}", siteId,
            throwable.getMessage()));
  }
  
  @PatchMapping(value = "/{shippingMethodId}", produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<ShippingMethodsResponse>> updateShippingMethod(
      @PathVariable @NotBlank(message = REQUIRED_FIELD_CODE) @Pattern(regexp = PatternConstants.UUID_PATTERN, message = INVALID_FIELD_CODE) String shippingMethodId,
      @Valid @RequestBody ShippingPatchRequest shippingPatchRequest) {
    return shippingService.updateShippingMethod(shippingPatchRequest, UUID.fromString(shippingMethodId))
        .doFirst(() -> log.info("Request received to update shipping method for shippingMethodId {}", shippingMethodId))
        .map(shippingMethods -> ResponseEntity.ok().body(shippingMethods))
        .doOnSuccess(id -> log.info("Shipping method updated successfully shippingMethodId : {}",shippingMethodId))
        .doOnError(throwable -> log.error("Error occurred while updating shipping methods for shippingMethodId : {} : {}", shippingMethodId, throwable.getMessage()));
  }
  
  @DeleteMapping(value = "/{shippingMethodId}/carrierString/{carrierStringId}", produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<String>> deleteShippingMethodCarrierString(
      @PathVariable @NotBlank(message = REQUIRED_FIELD_CODE) @Pattern(regexp = PatternConstants.UUID_PATTERN, message = INVALID_FIELD_CODE) String shippingMethodId,
      @PathVariable @NotBlank(message = REQUIRED_FIELD_CODE) @Pattern(regexp = PatternConstants.UUID_PATTERN, message = INVALID_FIELD_CODE) String carrierStringId) { 
    return shippingCarrierStringService.deleteShippingMethodCarrierString(UUID.fromString(shippingMethodId), UUID.fromString(carrierStringId))
        .then(Mono.just(ResponseEntity.noContent().header("Content-Length", "0").<String>build()))
        .doOnSuccess(id -> log.info("ShippingMethodCarrierString deleted successfully"))
        .doOnError(throwable -> log.error("Error occurred while deleting ShippingMethodCarrierString {} ", throwable.getMessage()));
  }
}
