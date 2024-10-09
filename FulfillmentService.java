package com.domainname.next.shippingapi.service;

import static com.domainname.next.shippingapi.constant.ErrorConstants.RECORD_NOT_FOUND_CODE;
import static com.domainname.next.shippingapi.constant.ErrorConstants.SITEID_NOT_FOUND_CODE;

import java.util.List;
import java.util.UUID;

import com.domainname.next.shippingapi.repository.FulfillmentMethodsRepository;
import com.domainname.next.shippingapi.repository.SiteIdRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import com.domainname.next.shippingapi.converter.FulfillmentMethodPostRequestConverter;
import com.domainname.next.shippingapi.converter.FulfillmentPatchRequestConverter;
import com.domainname.next.shippingapi.converter.FulfillmentResponseConverter;
import com.domainname.next.shippingapi.entity.FulfillmentMethods;
import com.domainname.next.shippingapi.entity.SiteId;
import com.domainname.next.shippingapi.exception.NotFoundException;
import com.domainname.next.shippingapi.resources.request.FulfillmentMethodPostRequest;
import com.domainname.next.shippingapi.resources.request.FulfillmentPatchRequest;
import com.domainname.next.shippingapi.resources.response.FulfillmentMethodsResponse;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class FulfillmentService {

  private final FulfillmentResponseConverter fulfillmentResponseConverter;
  private final FulfillmentMethodPostRequestConverter fulfillmentMethodPostRequestConverter;
  private final FulfillmentPatchRequestConverter fulfillmentPatchRequestConverter;
  private final FulfillmentMethodsRepository fulfillmentMethodsRepository;
  private final SiteIdRepository siteIdRepository;
  
  public FulfillmentService(FulfillmentResponseConverter fulfillmentResponseConverter,
      FulfillmentMethodPostRequestConverter fulfillmentMethodPostRequestConverter,
      FulfillmentPatchRequestConverter fulfillmentPatchRequestConverter,
      FulfillmentMethodsRepository fulfillmentMethodsRepository,
      SiteIdRepository siteIdRepository) {
    this.fulfillmentResponseConverter = fulfillmentResponseConverter;
    this.fulfillmentMethodPostRequestConverter = fulfillmentMethodPostRequestConverter;
    this.fulfillmentPatchRequestConverter = fulfillmentPatchRequestConverter;
    this.fulfillmentMethodsRepository = fulfillmentMethodsRepository;
    this.siteIdRepository = siteIdRepository;
  }

  public Mono<List<FulfillmentMethodsResponse>> getFulfillmentMethods(String siteId) {
    return fulfillmentMethodsRepository.findBySiteIdName(siteId)
        .doFirst(() -> log.debug("Processing get fulfillment request for siteId : {}", siteId))
        .switchIfEmpty(Mono.error(
            new NotFoundException(new HttpClientErrorException(HttpStatus.NOT_FOUND), SITEID_NOT_FOUND_CODE, siteId)))
        .collectList()
        .flatMap(response->fulfillmentResponseConverter.buildFulfillmentMethodsResponse(response, siteId));
  }

  public Mono<FulfillmentMethodsResponse> createFulfillmentMethods(FulfillmentMethodPostRequest fulfillmentRequests) {
    String siteId = fulfillmentRequests.getSiteId();
    return siteIdRepository.findByName(siteId)
        .flatMap(fulfillment -> fulfillmentMethodsRepository.save(fulfillmentMethodPostRequestConverter.apply(fulfillmentRequests, fulfillment))
            .doFirst(() -> log.debug("Processing create Fulfillment Methods request : {}", siteId))
            .flatMap(response->fulfillmentResponseConverter.buildFulfillmentMethodsResponse(response, siteId)));
  }
 
  public Mono<FulfillmentMethodsResponse> updateFulfillmentMethod(FulfillmentPatchRequest fulfillmentPatchRequest,
      UUID fulfillmentId) { 
    Mono<FulfillmentMethods> fulfillmentMethod = fulfillmentMethodsRepository.getByFulfillmentId(fulfillmentId);
    Mono<SiteId> siteId = fulfillmentMethod.flatMap(fulfillment -> siteIdRepository.findById(fulfillment.getSiteId()));
    return Mono.zip(fulfillmentMethod, siteId)
        .doFirst(() -> log.debug("Processing update fulfillment method request for fulfillment id : {}", fulfillmentId))
        .switchIfEmpty(Mono
            .error(new NotFoundException(new HttpClientErrorException(HttpStatus.NOT_FOUND), RECORD_NOT_FOUND_CODE,fulfillmentId.toString())))
        .flatMap(tuple -> fulfillmentPatchRequestConverter.apply(tuple.getT1(), fulfillmentPatchRequest)
        .flatMap(fulfillmentMethodsRepository::save)
        .flatMap(fulfillmentResponse -> fulfillmentResponseConverter.buildFulfillmentMethodsResponse(fulfillmentResponse, tuple.getT2().getName())));
  }
  
}
