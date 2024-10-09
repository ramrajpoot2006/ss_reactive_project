package com.domainname.next.shippingapi.service;


import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.domainname.next.shippingapi.converter.ShippingMethodResponseConverter;
import com.domainname.next.shippingapi.converter.ShippingPatchRequestConverter;
import com.domainname.next.shippingapi.entity.ShippingMethodCarrierString;
import com.domainname.next.shippingapi.entity.ShippingMethods;
import com.domainname.next.shippingapi.entity.SiteId;
import com.domainname.next.shippingapi.repository.ChannelRepository;
import com.domainname.next.shippingapi.repository.ProductTypeRepository;
import com.domainname.next.shippingapi.repository.ShippingMethodRepository;
import com.domainname.next.shippingapi.resources.request.ShippingMethodPostRequest;
import com.domainname.next.shippingapi.resources.request.ShippingPatchRequest;
import com.domainname.next.shippingapi.resources.response.ShippingMethodsResponse;
import com.domainname.next.shippingapi.resources.response.rule.ShippingMethodsRuleResponse;

import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;


@AllArgsConstructor
@Service
public class ShippingServiceBaseHandler {

  private final ChannelRepository channelRepository;

  private final ProductTypeRepository productTypeRepository;

  private final ShippingServiceHelper shippingServiceHelper;

  private final ShippingMethodRepository shippingMethodRepository;

  private final ShippingMethodResponseConverter shippingMethodResponseConverter;

  private final ShippingPatchRequestConverter shippingPatchRequestConverter;

  private final ShippingServiceBaseHelper shippingServiceBaseHelper;


  public Mono<ShippingMethodsResponse> buildShippingMethodResponse (
      ShippingPatchRequest shippingPatchRequest,
      SiteId siteId,
      ShippingMethods convertedShippingMethod,
      ShippingMethods shippingMethods,
      List<ShippingMethodCarrierString> allCarrierStrings
  ) {

    List<ShippingMethodCarrierString> newCarrierStrings = new ArrayList<>();
    return shippingServiceHelper.updateChannels(convertedShippingMethod.getChannels(), shippingMethods)
        .flatMap(channels -> shippingServiceHelper.updateProductTypes(
                convertedShippingMethod.getProductTypes(),
                shippingMethods
            )
            .flatMap(productTypes -> shippingServiceBaseHelper.updateCarrierString(
                    shippingPatchRequest,
                    shippingMethods,
                    newCarrierStrings
                )
                .flatMap(isCarrierStrings -> shippingMethodRepository.save(convertedShippingMethod)
                    .flatMap(shippingMethodResponse -> shippingMethodResponseConverter.buildShippingMethodsResponse(
                        shippingMethodResponse,
                        siteId.getName(),
                        channels,
                        productTypes,
                        newCarrierStrings,
                        allCarrierStrings
                    )))));
  }


  public Mono<ShippingMethods> buildConvertedShippingMethod (
      ShippingMethods shipMethod,
      List<ShippingMethodCarrierString> carrierStringList,
      ShippingPatchRequest shippingPatchRequest
  ) {

    return channelRepository.findChannelByShippingId(shipMethod.getId()).collectList()
        .flatMap(channelList -> productTypeRepository.findProductTypeByShippingId(shipMethod.getId()).collectList()
            .flatMap(productTypeList -> shippingPatchRequestConverter.apply(shipMethod,
                                                                            shippingPatchRequest,
                                                                            channelList,
                                                                            productTypeList,
                                                                            carrierStringList
            )));
  }


  public Mono<ShippingMethodsResponse> saveShippingMethodOptions (
      ShippingMethodPostRequest shippingRequest,
      ShippingMethods shippingMethods,
      String siteId
  ) {

    return shippingServiceHelper.saveChannels(shippingRequest, shippingMethods)
        .flatMap(channels -> shippingServiceHelper.saveProductTypes(shippingRequest, shippingMethods)
            .flatMap(productTypes -> shippingServiceBaseHelper.saveCarrierStrings(shippingRequest, shippingMethods)
                .flatMap(carrierStrings -> shippingMethodResponseConverter
                    .buildShippingMethodsResponse(
                        shippingMethods,
                        siteId,
                        channels,
                        productTypes,
                        carrierStrings,
                        new ArrayList<>()
                    ))));

  }


  public Mono<List<ShippingMethodsRuleResponse>> getShippingMethods (List<ShippingMethodsRuleResponse> shippingMethodsRuleResponse) {

    List<Integer> shippingMethodIds = shippingServiceBaseHelper.getShippingMethodIds(shippingMethodsRuleResponse);
    return shippingServiceHelper.getShippingMethodsRules(shippingMethodsRuleResponse, shippingMethodIds)
        .flatMap(shippingMethodRuleResponse -> shippingServiceBaseHelper.getShippingMethodCarrierString(
            shippingMethodRuleResponse,
            shippingMethodIds
        ));
  }
}
