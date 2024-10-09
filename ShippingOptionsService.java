package com.domainname.next.shippingapi.service;

import static com.domainname.next.shippingapi.constant.ErrorConstants.NOT_FOUND_ANY_FULFILLMENT_METHOD;
import static com.domainname.next.shippingapi.constant.ErrorConstants.NOT_FOUND_CODE;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import com.domainname.next.shippingapi.entity.FulfillmentOption;
import com.domainname.next.shippingapi.entity.SiteId;
import com.domainname.next.shippingapi.enums.FulfillmentType;
import com.domainname.next.shippingapi.enums.LineType;
import com.domainname.next.shippingapi.exception.NotFoundException;
import com.domainname.next.shippingapi.repository.FulfillmentMethodsRepository;
import com.domainname.next.shippingapi.repository.SiteIdRepository;
import com.domainname.next.shippingapi.resources.request.ProductLine;
import com.domainname.next.shippingapi.resources.request.ShippingOptionsPostRequest;
import com.domainname.next.shippingapi.resources.response.ShippingOptionsResponse;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class ShippingOptionsService extends ShippingOptionsBaseService {

  private static final String CNC = FulfillmentType.CLICKANDCOLLECT.getValue();
  private static final String HOMEDELIVERY = FulfillmentType.HOMEDELIVERY.getValue();
  private static final String PUDO = FulfillmentType.PUDO.getValue();
  private static final String DIGITAL = FulfillmentType.DIGITAL.getValue();

  private final ShippingOptionsServiceHelper shippingOptionsServiceHelper;

  public ShippingOptionsService(FulfillmentMethodsRepository fulfillmentMethodsRepository,
      SiteIdRepository siteIdRepository,
      ShippingOptionsServiceHelper shippingOptionsServiceHelper
  ) {
    super(fulfillmentMethodsRepository, siteIdRepository);
    this.shippingOptionsServiceHelper = shippingOptionsServiceHelper;
  }

  public Mono<List<ShippingOptionsResponse>> createShippingOptions(ShippingOptionsPostRequest shippingOptionsRequest,
      List<String> embed) {
    log.info("Processing createShippingOptions for shippingOptionsRequest : {}", shippingOptionsRequest);
    List<ShippingOptionsResponse> shippingOptionsResponseList = new ArrayList<>();
    String siteId = shippingOptionsRequest.getSiteId();
    List<String> fulfillmentTypes = getFulfillmentList(embed);
    List<ProductLine> productLinesDigital = getProductLinesDigital(shippingOptionsRequest);
    shippingOptionsRequest.setProductLines(shippingOptionsRequest.getProductLines().stream()
        .filter(productLine -> !LineType.EGIFTCARD.getValue().equals(productLine.getLineType()))
        .toList());
    return getSiteId(siteId).flatMap(siteIdResponse -> getFulfillmentOptions(siteIdResponse, fulfillmentTypes)
        .doFirst(() -> log.debug("Processing Shipping Options request for siteId : {}", siteId))
        .flatMap(fulfillmentOptionMap -> Optional.ofNullable(embed).filter(Predicate.not(List::isEmpty))
            .map(validMap -> validateFulfillmentOptionsForSiteId(fulfillmentOptionMap, embed, siteId)).orElse(Flux.just(""))
            .flatMap(fulfillmentType -> checkFulfillmentOption(shippingOptionsRequest, productLinesDigital,
                fulfillmentOptionMap, siteIdResponse, fulfillmentType, shippingOptionsResponseList))
            .switchIfEmpty(Mono.defer(() -> createAllShippingOptions(shippingOptionsRequest,
                productLinesDigital, shippingOptionsResponseList, fulfillmentOptionMap, siteIdResponse)))
            .then(Mono.just(shippingOptionsResponseList))
            .map(shippingOptionsResponse -> sortShippingOptionsResponse(shippingOptionsResponse,fulfillmentOptionMap))));
  }

  private Mono<Boolean> createAllShippingOptions(ShippingOptionsPostRequest shippingOptionsRequest,
      List<ProductLine> productLinesDigital,
      List<ShippingOptionsResponse> shippingOptionsResponseList, Map<String, FulfillmentOption> fulfillmentOptionMap,
      SiteId siteId) {
    return Flux.fromIterable(fulfillmentOptionMap.keySet())
        .flatMap(fulfillmentType -> {
          if (fulfillmentType.equals(HOMEDELIVERY) || fulfillmentType.equals(CNC) || fulfillmentType.equals(PUDO)
              || fulfillmentType.equals(DIGITAL)) {
            return createShippingOption(shippingOptionsRequest, productLinesDigital, fulfillmentType,
                                        fulfillmentOptionMap, siteId).map(shippingOptionsResponseList::add);
          }
          return Mono.empty();
        }).switchIfEmpty(Mono.error(new NotFoundException(new HttpClientErrorException(HttpStatus.NOT_FOUND)
            , NOT_FOUND_ANY_FULFILLMENT_METHOD, siteId.getName())))
        .then(Mono.just(true));
  }

  private Mono<ShippingOptionsResponse> createShippingOption(ShippingOptionsPostRequest shippingOptionsRequest,
      List<ProductLine> productLinesDigital, String fulfillmentType,
      Map<String, FulfillmentOption> fulfillmentOptionMap, SiteId siteIdResponse) {
    if (fulfillmentType.equals(HOMEDELIVERY)) {
      return shippingOptionsServiceHelper.createDPEHD(shippingOptionsRequest, siteIdResponse,
          fulfillmentOptionMap);
    } else if (fulfillmentType.equals(CNC)) {
      return shippingOptionsServiceHelper.createCNCDPE(shippingOptionsRequest, siteIdResponse,
          fulfillmentOptionMap, productLinesDigital);
    } else {
      return createPUDOorDigital(shippingOptionsRequest, productLinesDigital, fulfillmentType,
          fulfillmentOptionMap, siteIdResponse);
    }
  }

  private Mono<ShippingOptionsResponse> createPUDOorDigital(ShippingOptionsPostRequest shippingOptionsRequest,
      List<ProductLine> productLinesDigital, String fulfillmentType,
      Map<String, FulfillmentOption> fulfillmentOptionMap, SiteId siteIdResponse) {
    if (fulfillmentType.equals(PUDO)) {
      return shippingOptionsServiceHelper.createDPEPUDO(shippingOptionsRequest, fulfillmentOptionMap,
          siteIdResponse);
    } else {
      return shippingOptionsServiceHelper.createDigital(shippingOptionsRequest, productLinesDigital, siteIdResponse, fulfillmentOptionMap);
    }
  }

  public Flux<String> validateFulfillmentOptionsForSiteId(Map<String, FulfillmentOption> fulfillmentOptions, List<String> embed,
      String siteId) {
    return Flux.fromIterable(fulfillmentOptions.values())
        .filter(fulfillmentOption -> embed.contains(fulfillmentOption.getFulfillmentType()))
        .map(FulfillmentOption::getFulfillmentType)
        .switchIfEmpty(Mono.error(new NotFoundException(new HttpClientErrorException(HttpStatus.NOT_FOUND),
                                                        NOT_FOUND_CODE, embed.get(0), siteId)));
  }

  private Mono<Boolean> checkFulfillmentOption(ShippingOptionsPostRequest shippingOptionsRequest,
      List<ProductLine> productLinesDigital, Map<String, FulfillmentOption> fulfillmentOptionMap,
      SiteId siteIdResponse, String fulfillmentType, List<ShippingOptionsResponse> shippingOptionsResponseList) {
    if (HOMEDELIVERY.equals(fulfillmentType) || CNC.equals(fulfillmentType) || PUDO.equals(fulfillmentType)
        || DIGITAL.equals(fulfillmentType)) {
      return createShippingOption(shippingOptionsRequest, productLinesDigital, fulfillmentType,
          fulfillmentOptionMap, siteIdResponse).map(shippingOptionsResponseList::add);
    }
    return Mono.empty();
  }
  
  private static List<ProductLine> getProductLinesDigital(ShippingOptionsPostRequest shippingOptionsRequest) {
    return shippingOptionsRequest.getProductLines().stream()
        .filter(productLine -> LineType.EGIFTCARD.getValue().equals(productLine.getLineType()))
        .toList();
  }
} 
