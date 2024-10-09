package com.domainname.next.shippingapi.controller;

import static com.domainname.next.shippingapi.constant.ErrorConstants.INVALID_FIELD_CODE;

import java.util.List;

import javax.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.domainname.next.shippingapi.annotation.FulfillmentCollectionPattern;
import com.domainname.next.shippingapi.enums.FulfillmentType;
import com.domainname.next.shippingapi.resources.request.ShippingOptionsPostRequest;
import com.domainname.next.shippingapi.resources.response.FulfillmentOptionsResponse;
import com.domainname.next.shippingapi.service.FulfillmentOptionsService;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/fulfillment-options")
@Slf4j
@Validated
public class FulfillmentOptionsController {

  private final FulfillmentOptionsService fulfillmentOptionsService;

  public FulfillmentOptionsController(FulfillmentOptionsService fulfillmentOptionsService) {
    this.fulfillmentOptionsService = fulfillmentOptionsService;
  }

  @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<List<FulfillmentOptionsResponse>>> createFulfillmentOptions(
      @Valid @RequestBody ShippingOptionsPostRequest shippingOptionsRequest,
      @FulfillmentCollectionPattern(targetClassType = FulfillmentType.class, message = INVALID_FIELD_CODE) @RequestParam(value = "embed", required = false) List<String> embed) {
    String siteId = shippingOptionsRequest.getSiteId();
    return fulfillmentOptionsService.createFulfillmentOptions(shippingOptionsRequest, embed)
        .doFirst(() -> log.info("Request received to get fulfillment Options {}", siteId))
        .map(fulfillmentOptions -> ResponseEntity.status(HttpStatus.OK).body(fulfillmentOptions))
        .doOnSuccess(request -> log.info("Fulfillment options retrieved successfully : {}", siteId))
        .doOnError(throwable -> log.error("Error in retrieving fulfillment options : {}", siteId));
  }
}
