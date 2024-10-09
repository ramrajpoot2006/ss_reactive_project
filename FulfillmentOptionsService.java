package com.domainname.next.shippingapi.service;

import com.domainname.next.shippingapi.entity.FulfillmentOption;
import com.domainname.next.shippingapi.entity.SiteId;
import com.domainname.next.shippingapi.enums.FulfillmentType;
import com.domainname.next.shippingapi.enums.LineType;
import com.domainname.next.shippingapi.exception.NotFoundException;
import com.domainname.next.shippingapi.repository.FulfillmentMethodsRepository;
import com.domainname.next.shippingapi.repository.SiteIdRepository;
import com.domainname.next.shippingapi.resources.request.ProductLine;
import com.domainname.next.shippingapi.resources.request.ShippingOptionsPostRequest;
import com.domainname.next.shippingapi.resources.response.FulfillmentOptionsResponse;
import com.domainname.next.shippingapi.util.ShippingOptionsUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.domainname.next.shippingapi.constant.ErrorConstants.NOT_FOUND_ANY_FULFILLMENT_METHOD;
import static com.domainname.next.shippingapi.constant.ErrorConstants.NOT_FOUND_CODE;

@AllArgsConstructor
@Service
@Slf4j
public class FulfillmentOptionsService {

  private final SiteIdRepository siteIdRepository;
  private final FulfillmentMethodsRepository fulfillmentMethodsRepository;
  private final FulfillmentOptionsServiceHelper fulfillmentOptionsServiceHelper;


  public Mono<List<FulfillmentOptionsResponse>> createFulfillmentOptions (
      ShippingOptionsPostRequest shippingOptionsRequest,
      List<String> embed
  ) {

    log.info("Processing createFulfillmentOptions for shippingOptionsRequest : {}", shippingOptionsRequest);
    String siteId = shippingOptionsRequest.getSiteId();
    List<String> fulfillmentTypes = Stream.of(FulfillmentType.values()).map(FulfillmentType::getValue)
        .toList();
    return siteIdRepository.findByName(siteId)
        .zipWhen(siteIdResponse -> this.getEnabledFulfillmentOptionsForSiteIdMap(
            siteIdResponse,
            fulfillmentTypes
        ))
        .doFirst(() -> log.debug("Processing Fulfillment Options request for siteId : {}", siteId))
        .flatMap(siteIdAndEnabledFulfillmentOptionsTuple ->
                     Optional.ofNullable(embed)
                         .filter(Predicate.not(List::isEmpty))
                         .map(selectedEmbedOptions ->
                                  this.createSelectedFulfillmentOptions(
                                      shippingOptionsRequest,
                                      siteIdAndEnabledFulfillmentOptionsTuple.getT1(),
                                      siteIdAndEnabledFulfillmentOptionsTuple.getT2(),
                                      selectedEmbedOptions
                                  ))
                         .orElseGet(() -> this.createAllFulfillmentOptions(
                             shippingOptionsRequest,
                             siteIdAndEnabledFulfillmentOptionsTuple.getT1(),
                             siteIdAndEnabledFulfillmentOptionsTuple.getT2()
                         ))
                         .collectList()
                         .map(shippingOptionsResponse -> fulfillmentOptionsServiceHelper.sortFulfillmentOptionsResponse(
                             shippingOptionsResponse,
                             siteIdAndEnabledFulfillmentOptionsTuple.getT2()
                         )));
  }

  private Mono<Map<String, FulfillmentOption>> getEnabledFulfillmentOptionsForSiteIdMap(SiteId siteId, List<String> fulfillmentTypes) {
    return fulfillmentMethodsRepository.findBySiteIdAndFulfilmentType(siteId.getId(), fulfillmentTypes)
        .filter(fulfillmentOption -> isFulfillmentEnabled(fulfillmentOption.getFulfillmentType(), siteId.getFulfillmentMethodsEnabled()))
        .collectList()
        .map(fulfillmentOptionsServiceHelper::prepareFulfillmentOptions);
  }

  private static boolean isFulfillmentEnabled(String fulfillmentName, List<String> fulfillmentMethodsEnabled) {
    return fulfillmentMethodsEnabled.contains(fulfillmentName);
  }

  private Flux<FulfillmentOptionsResponse> createSelectedFulfillmentOptions(ShippingOptionsPostRequest shippingOptionsRequest,
                                                                            SiteId siteId,
                                                                            Map<String, FulfillmentOption> enabledFulfillmentOptions,
                                                                            List<String> selectedFulfillmentOptions) {
    List<ProductLine> digitalProductLines = getDigitalProductLines(shippingOptionsRequest);
    shippingOptionsRequest.setProductLines(getProductLinesWithoutDigitalType(shippingOptionsRequest));
    return Flux.fromIterable(selectedFulfillmentOptions)
        .filter(enabledFulfillmentOptions::containsKey)
        .switchIfEmpty(Mono.error(new NotFoundException(new HttpClientErrorException(HttpStatus.NOT_FOUND),
            NOT_FOUND_CODE, selectedFulfillmentOptions.get(0), siteId.getName())))
        .flatMap(fulfillmentType -> createFulfillmentOption(shippingOptionsRequest, digitalProductLines, fulfillmentType,
            enabledFulfillmentOptions.get(fulfillmentType), siteId));
  }

  private Flux<FulfillmentOptionsResponse> createAllFulfillmentOptions(ShippingOptionsPostRequest shippingOptionsRequest,
                                                                       SiteId siteId,
                                                                       Map<String, FulfillmentOption> enabledFulfillmentOptions) {
    List<ProductLine> digitalProductLines = getDigitalProductLines(shippingOptionsRequest);
    shippingOptionsRequest.setProductLines(getProductLinesWithoutDigitalType(shippingOptionsRequest));
    return Flux.fromIterable(enabledFulfillmentOptions.keySet())
        .flatMap(fulfillmentType -> createFulfillmentOption(shippingOptionsRequest, digitalProductLines, fulfillmentType,
            enabledFulfillmentOptions.get(fulfillmentType), siteId))
        .switchIfEmpty(Mono.error(new NotFoundException(new HttpClientErrorException(HttpStatus.NOT_FOUND)
            , NOT_FOUND_ANY_FULFILLMENT_METHOD, siteId.getName())));
  }

  private Mono<FulfillmentOptionsResponse> createFulfillmentOption(ShippingOptionsPostRequest shippingOptionsRequest,
                                                                   List<ProductLine> digitalProductLines,
                                                                   String fulfillmentType,
                                                                   FulfillmentOption fulfillmentOptionConfig,
                                                                   SiteId siteId) {
    if (fulfillmentType.equals(FulfillmentType.DIGITAL.getValue())) {
      return fulfillmentOptionsServiceHelper.createDigital(shippingOptionsRequest, digitalProductLines,
                                                           fulfillmentOptionConfig, siteId);
    } else {
      return createNonDigitalFulfillmentOption(shippingOptionsRequest, fulfillmentType, fulfillmentOptionConfig, siteId);
    }
  }

  private Mono<FulfillmentOptionsResponse> createNonDigitalFulfillmentOption(ShippingOptionsPostRequest shippingOptionsRequest,
                                                                             String fulfillmentType,
                                                                             FulfillmentOption fulfillmentOptionConfig,
                                                                             SiteId siteId) {
    if (fulfillmentType.equals(FulfillmentType.HOMEDELIVERY.getValue())) {
      return ShippingOptionsUtil.buildFulfillmentOptionsEmptyResponse(fulfillmentOptionConfig, shippingOptionsRequest);
    } else if (fulfillmentType.equals(FulfillmentType.CLICKANDCOLLECT.getValue())) {
      return ShippingOptionsUtil.buildFulfillmentOptionsEmptyResponse(fulfillmentOptionConfig, shippingOptionsRequest);
    } else  {
      return fulfillmentOptionsServiceHelper.createDPEPUDO(shippingOptionsRequest, fulfillmentOptionConfig, siteId);
    }
  }

  private static List<ProductLine> getDigitalProductLines(ShippingOptionsPostRequest shippingOptionsRequest) {
    return shippingOptionsRequest.getProductLines().stream()
        .filter(productLine -> LineType.EGIFTCARD.getValue().equals(productLine.getLineType()))
        .toList();
  }

  private static List<ProductLine> getProductLinesWithoutDigitalType(ShippingOptionsPostRequest shippingOptionsRequest) {
    return shippingOptionsRequest.getProductLines().stream()
        .filter(productLine -> !LineType.EGIFTCARD.getValue().equals(productLine.getLineType()))
        .toList();
  }

}
