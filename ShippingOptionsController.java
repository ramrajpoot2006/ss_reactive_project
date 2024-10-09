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
import com.domainname.next.shippingapi.resources.response.ShippingOptionsResponse;
import com.domainname.next.shippingapi.service.ShippingOptionsService;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/shipping-options")
@Slf4j
@Validated
public class ShippingOptionsController {

  private final ShippingOptionsService shippingOptionsService;

  public ShippingOptionsController(ShippingOptionsService shippingOptionsService) {
    this.shippingOptionsService = shippingOptionsService;
  }

  @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<List<ShippingOptionsResponse>>> createShippingOptions(
      @Valid @RequestBody ShippingOptionsPostRequest shippingOptionsRequest,
      @FulfillmentCollectionPattern(targetClassType = FulfillmentType.class, message = INVALID_FIELD_CODE)
      @RequestParam(value = "embed", required = false) List<String> embed) {
    String siteId = shippingOptionsRequest.getSiteId();
    return shippingOptionsService.createShippingOptions(shippingOptionsRequest,embed)
        .doFirst(() -> log.info("Request received to get shipping Options {}", siteId))
        .map(shippingOptions -> ResponseEntity.status(HttpStatus.OK).body(shippingOptions))
        .doOnEach(request -> log.info("Shipping options retrieved successfully : {}", siteId))
        .doOnError(throwable -> log.error("Error in retrieving shipping options : {}", siteId));
  }
}
