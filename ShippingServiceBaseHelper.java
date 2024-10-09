package com.domainname.next.shippingapi.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.domainname.next.shippingapi.repository.ShippingMethodCarrierStringRepository;
import lombok.AllArgsConstructor;
import com.domainname.next.shippingapi.entity.ShippingMethodRule;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.domainname.next.shippingapi.converter.ShippingMethodPostRequestConverter;
import com.domainname.next.shippingapi.entity.ShippingMethodCarrierString;
import com.domainname.next.shippingapi.entity.ShippingMethods;
import com.domainname.next.shippingapi.resources.request.ShippingMethodPostRequest;
import com.domainname.next.shippingapi.resources.request.ShippingPatchRequest;
import com.domainname.next.shippingapi.resources.response.CarrierStringRecord;
import com.domainname.next.shippingapi.resources.response.ShippingMethodCarrierStringRecord;
import com.domainname.next.shippingapi.resources.response.rule.ShippingMethodsRuleResponse;

import io.micrometer.core.instrument.util.StringUtils;
import reactor.core.publisher.Mono;

@Component
@AllArgsConstructor
public class ShippingServiceBaseHelper {
  
  private final ShippingMethodPostRequestConverter shippingMethodPostRequestConverter;
  private final ShippingMethodCarrierStringRepository shippingMethodCarrierStringRepository;


  public Mono<List<ShippingMethodsRuleResponse>> getShippingMethodCarrierString(
      List<ShippingMethodsRuleResponse> shippingMethodsRuleResponse, List<Integer> shippingMethodIds) {
    return shippingMethodCarrierStringRepository.findCarrierStringRecordsByShippingMethodIdIn(shippingMethodIds).collectList()
        .flatMap(shippingMethod -> shippingMethodPostRequestConverter.buildShippingMethodCarrierString(
            shippingMethodsRuleResponse, carrierStringMapping(shippingMethod)));
  }
  
  public List<Integer> getShippingMethodIds(Collection<ShippingMethodsRuleResponse> shippingMethodsRuleResponse) {
    return shippingMethodsRuleResponse.stream().map(ShippingMethodsRuleResponse::getId)
        .collect(Collectors.toList());
  }
  
  public List<Integer> getRuleIds(Collection<ShippingMethodRule> shippingMethodsRules) {
    return shippingMethodsRules.stream().map(ShippingMethodRule::getId).collect(Collectors.toList());
  }
  
  private static Map<Integer, List<CarrierStringRecord>> carrierStringMapping(
      Collection<ShippingMethodCarrierStringRecord> shippingMethodCarrierStringRecords) {
    return shippingMethodCarrierStringRecords.stream()
        .collect(Collectors.groupingBy(ShippingMethodCarrierStringRecord::getShippingMethodId,
            Collectors
                .mapping(carrierString -> CarrierStringRecord.builder().carrierString(carrierString.getCarrierString())
                    .carrierStringId(carrierString.getCarrierStringId()).build(), Collectors.toList())));
  }

  public Mono<List<ShippingMethodCarrierString>> saveCarrierStrings(ShippingMethodPostRequest shippingRequest,
      ShippingMethods shippingMethods) {
    return CollectionUtils.isEmpty(shippingRequest.getCarrierStringRecords())
        ? Mono.just(new ArrayList<>())
        : shippingMethodPostRequestConverter.buildCarrierString(shippingRequest, shippingMethods)
            .flatMap(shippingMethodCarrierString -> shippingMethodCarrierStringRepository
                .saveAll(shippingMethodCarrierString).collectList());
  }

  public Mono<Boolean> updateCarrierString(ShippingPatchRequest shippingPatchRequest,
      ShippingMethods shippingMethods, Collection<ShippingMethodCarrierString> newCarrierStrings) {
    List<CarrierStringRecord> carrierStringUpdateList = new ArrayList<>();
    List<CarrierStringRecord> carrierStringInsertList = new ArrayList<>();
    if (Optional.ofNullable(shippingPatchRequest.getCarrierStringRecords()).isPresent()) {
      shippingPatchRequest.getCarrierStringRecords().forEach(carrierString -> {
        if (!StringUtils.isEmpty(carrierString.getCarrierStringId())) {
          carrierStringUpdateList.add(carrierString);
        } else {
          carrierStringInsertList.add(carrierString);
        }
      });
    }
    return updateCarrierStringsPatch(carrierStringUpdateList, shippingMethods)
        .map(newCarrierStrings::addAll)
        .flatMap(carrierString -> saveCarrierStringsPatch(carrierStringInsertList, shippingMethods))
        .map(newCarrierStrings::addAll);
  }

  public Mono<List<ShippingMethodCarrierString>> updateCarrierStringsPatch(
      List<CarrierStringRecord> carrierStringUpdateList, ShippingMethods shippingMethods) {
    return CollectionUtils.isEmpty(carrierStringUpdateList)
           ? Mono.just(new ArrayList<>()) : shippingMethodPostRequestConverter.buildCarrierStringForUpdate(carrierStringUpdateList, shippingMethods)
           .flatMap(shippingMethodCarrierString -> shippingMethodCarrierStringRepository.saveAll(shippingMethodCarrierString).collectList());
  }

  public Mono<List<ShippingMethodCarrierString>> saveCarrierStringsPatch(
      List<CarrierStringRecord> carrierStringInsertList, ShippingMethods shippingMethods) {
    return CollectionUtils.isEmpty(carrierStringInsertList)
           ? Mono.just(new ArrayList<>()) : shippingMethodPostRequestConverter.buildCarrierStringForSave(carrierStringInsertList, shippingMethods)
           .flatMap(shippingMethodCarrierString -> shippingMethodCarrierStringRepository.saveAll(shippingMethodCarrierString).collectList());
  }
}
