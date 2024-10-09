package com.domainname.next.shippingapi.service;


import com.domainname.next.shippingapi.converter.ShippingMethodPostRequestConverter;
import com.domainname.next.shippingapi.entity.Channel;
import com.domainname.next.shippingapi.entity.ProductType;
import com.domainname.next.shippingapi.entity.ShippingMethods;
import com.domainname.next.shippingapi.repository.ChannelRepository;
import com.domainname.next.shippingapi.repository.ProductTypeRepository;
import com.domainname.next.shippingapi.repository.ShippingMethodChannelMappingRepository;
import com.domainname.next.shippingapi.repository.ShippingMethodProductTypeMappingRepository;
import com.domainname.next.shippingapi.resources.request.ShippingMethodPostRequest;
import com.domainname.next.shippingapi.resources.response.rule.ShippingMethodsRuleResponse;
import com.domainname.next.shippingapi.util.ShippingUtil;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;


@Component
@AllArgsConstructor
public class ShippingServiceHelper {

  private final ShippingMethodRuleHelper shippingMethodRuleHelper;

  private final ShippingMethodPostRequestConverter shippingMethodPostRequestConverter;

  private final ChannelRepository channelRepository;

  private final ProductTypeRepository productTypeRepository;

  private final ShippingMethodProductTypeMappingRepository shippingMethodProductTypeMappingRepository;

  private final ShippingMethodChannelMappingRepository shippingMethodChannelMappingRepository;


  public Mono<List<Channel>> saveChannels (ShippingMethodPostRequest shippingRequest, ShippingMethods shippingMethods) {

    return channelRepository
        .findByChannelNameIn(ShippingUtil.getEnums(
            com.domainname.next.shippingapi.enums.Channel.class,
            shippingRequest.getChannels()
        ))
        .collectList()
        .flatMap(channelConfig -> shippingMethodPostRequestConverter.buildChannel(shippingMethods, channelConfig)
            .flatMap(channelMapping -> shippingMethodChannelMappingRepository.saveAll(channelMapping).collectList())
            .then(Mono.just(channelConfig)));
  }


  public Mono<List<ProductType>> saveProductTypes (
      ShippingMethodPostRequest shippingRequest,
      ShippingMethods shippingMethods
  ) {

    return productTypeRepository.findByProductTypeNameIn(shippingRequest.getProductTypes()).collectList()
        .flatMap(productTypeConfig -> shippingMethodPostRequestConverter.buildProductType(
                shippingMethods,
                productTypeConfig
            )
            .flatMap(productTypeMapping -> shippingMethodProductTypeMappingRepository.saveAll(productTypeMapping)
                .collectList())
            .then(Mono.just(productTypeConfig)));
  }


  public Mono<List<Channel>> updateChannels (List<String> channels, ShippingMethods shippingMethods) {

    return channelRepository.findByChannelNameIn(channels).collectList()
        .flatMap(channel -> shippingMethodPostRequestConverter.buildChannel(shippingMethods, channel)
            .flatMap(channelMapping -> shippingMethodChannelMappingRepository.deleteByShippingMethodId(shippingMethods.getId())
                .flatMap(deletedMapping -> shippingMethodChannelMappingRepository.saveAll(channelMapping).collectList())
                .then(Mono.just(channel))));
  }


  public Mono<List<ProductType>> updateProductTypes (List<String> productTypes, ShippingMethods shippingMethods) {

    return productTypeRepository.findByProductTypeNameIn(productTypes).collectList()
        .flatMap(productType -> shippingMethodPostRequestConverter.buildProductType(shippingMethods, productType)
            .flatMap(productTypeMapping -> shippingMethodProductTypeMappingRepository.deleteByShippingMethodId(
                    shippingMethods.getId())
                .flatMap(deletedMapping -> shippingMethodProductTypeMappingRepository.saveAll(productTypeMapping)
                    .collectList())
                .then(Mono.just(productType))));
  }


  public Mono<List<ShippingMethodsRuleResponse>> getShippingMethodsRules (
      List<ShippingMethodsRuleResponse> shippingMethodsRuleResponse, List<Integer> shippingMethodIds
  ) {

    return shippingMethodRuleHelper.getShippingMethodsRules(shippingMethodsRuleResponse, shippingMethodIds);
  }
}
