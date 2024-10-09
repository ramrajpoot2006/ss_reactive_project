package com.domainname.next.shippingapi.service;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.domainname.next.shippingapi.entity.FulfillmentOption;
import com.domainname.next.shippingapi.entity.SiteId;
import com.domainname.next.shippingapi.handler.digital.DigitalServiceHandler;
import com.domainname.next.shippingapi.handler.pudo.FulfillmentOptionsPUDOSiteIdHandler;
import com.domainname.next.shippingapi.handler.pudo.PUDOServiceHandler;
import com.domainname.next.shippingapi.resources.dto.CompleteLocation;
import com.domainname.next.shippingapi.resources.request.ProductLine;
import com.domainname.next.shippingapi.resources.request.ShippingOptionsPostRequest;
import com.domainname.next.shippingapi.resources.response.FulfillmentOptionsResponse;
import com.domainname.next.shippingapi.util.ShippingOptionsUtil;

import io.micrometer.core.instrument.util.StringUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;


@AllArgsConstructor
@Service
@Slf4j
public class FulfillmentOptionsServiceHelper {

  private final ShippingOptionsValidationService shippingOptionsValidationService;
  private final PUDOServiceHandler pudoServiceHandler;
  private final FulfillmentOptionsPUDOSiteIdHandler fulfillmentOptionsPUDOSiteIdHandler;
  private final DigitalServiceHandler digitalServiceHandler;

  public Mono<FulfillmentOptionsResponse> createDPEPUDO(ShippingOptionsPostRequest shippingOptionsRequest,
                                                         FulfillmentOption pudoFulfillmentOption,
                                                         SiteId siteIdResponse) {
    if (!StringUtils.isEmpty(shippingOptionsRequest.getPudoId())) {
      return pudoServiceHandler.createPUDO(shippingOptionsRequest, pudoFulfillmentOption,
          siteIdResponse, new CompleteLocation());
    }
    return shippingOptionsValidationService
        .validatePUDODPERequestAndFetchGeocodeResult(shippingOptionsRequest,
            siteIdResponse.getCountryName())
        .filter(geoCodeResult -> !CollectionUtils.isEmpty(geoCodeResult.getResults()))
        .flatMap(geoCodeResult -> shippingOptionsValidationService
            .validateGeocodeResult(shippingOptionsRequest, geoCodeResult, siteIdResponse.getCountryName()))
        .flatMap(completeLocation -> {
          if (shippingOptionsValidationService.isSiteIdCall(shippingOptionsRequest, completeLocation, siteIdResponse)) {
            return fulfillmentOptionsPUDOSiteIdHandler.createPUDOSiteIdFullfillmentOptionsResponse(
                pudoFulfillmentOption, shippingOptionsRequest, siteIdResponse);
          }
          return pudoServiceHandler.createPUDO(
              shippingOptionsRequest,
              pudoFulfillmentOption,
              siteIdResponse,
              completeLocation
          );
        })
        .switchIfEmpty(Mono.defer(() -> fulfillmentOptionsPUDOSiteIdHandler.createPUDOSiteIdFullfillmentOptionsResponse(
            pudoFulfillmentOption, shippingOptionsRequest, siteIdResponse)))
        .doOnError(throwable -> log.error(
            "Validation error occurred during DPE PUDO flow : {}",
            throwable.getMessage()
        ))
        .onErrorResume(throwable -> ShippingOptionsUtil.buildFulfillmentOptionsEmptyResponse(
            pudoFulfillmentOption,
            shippingOptionsRequest
        ));
  }

  public Mono<FulfillmentOptionsResponse> createDigital(ShippingOptionsPostRequest shippingOptionsRequest,
                                                        List<ProductLine> digitalProductLines,
                                                        FulfillmentOption digitalFulfillmentOption,
                                                        SiteId siteIdResponse) {

    return Optional.of(digitalProductLines)
        .filter(Predicate.not(List::isEmpty))
        .map(digitalProductLinesNotEmpty -> digitalServiceHandler.createDigitalFulfillmentOption(shippingOptionsRequest,
                                                                                                 digitalProductLines,
                                                                                                 digitalFulfillmentOption,
                                                                                                 siteIdResponse
        ))
        .orElseGet(() ->
                       ShippingOptionsUtil.buildFulfillmentOptionsEmptyResponse(
                           digitalFulfillmentOption,
                           shippingOptionsRequest
                       ));
  }

  public Map<String, FulfillmentOption> prepareFulfillmentOptions (Collection<FulfillmentOption> fulfillmentOptions) {

    return fulfillmentOptions.stream()
        .collect(Collectors.toMap(FulfillmentOption::getFulfillmentType,
                                  fulfillmentOption -> fulfillmentOption,
                                  (u, v) -> u,
                                  LinkedHashMap::new
        ));
  }

  public List<FulfillmentOptionsResponse> sortFulfillmentOptionsResponse (
      Collection<FulfillmentOptionsResponse> fulfillmentOptionsResponse,
      Map<String, FulfillmentOption> fulfillmentOptionMap
  ) {

    Map<String, FulfillmentOptionsResponse> fulfillmentOptionsResponseMap = fulfillmentOptionsResponse.stream().collect(Collectors.toMap(FulfillmentOptionsResponse::getFulfillmentType, r -> r));

    return fulfillmentOptionMap.keySet()
        .stream()
        .map(fulfillmentOptionsResponseMap::get)
        .filter(Objects::nonNull)
        .toList();
  }
}
