package com.domainname.next.shippingapi.repository;


import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import com.domainname.next.shippingapi.entity.ShippingMethods;
import com.domainname.next.shippingapi.resources.request.ShippingPrice;
import com.domainname.next.shippingapi.resources.response.ShippingMethodCarrierStringRecord;
import com.domainname.next.shippingapi.resources.response.rule.ShippingMethodsRuleResponse;
import com.domainname.next.shippingapi.resources.response.rule.ShippingMethodsRulesData;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ShippingMethodRepository extends ReactiveCrudRepository<ShippingMethods, Integer> {


  @Query("select count(smc.shipping_method_id) > 0 FROM site_shipping_method_configuration smc" +
      "   inner join shipping_method_channel_mapping smcm on (smc.id = smcm.shipping_method_id and smcm.channel_id in (select channel_id from channel where LOWER(channel_name) in (:channels)))  or length(concat(:channels)) = 0" +
      "   inner join shipping_method_product_type_mapping smpm on (smc.id = smpm.shipping_method_id and smpm.product_type_id in (select product_type_id from product_type where product_type_name in (:productTypes))) or length(concat(:productTypes)) = 0" +
      "   inner join shipping_method_carrier_string smcs on (smc.id = smcs.shipping_method_id and smcs.carrier_string in (:carrierStringRecords)) or length(concat(:carrierStringRecords)) = 0 " +
      " where smc.site_id = :siteId and smc.enabled = true and (smc.carrier_service=:carrierService or :carrierService is null)")
  Mono<Boolean> existsByUniqueSiteIdParameters(Integer siteId, List<String> channels, List<String> productTypes, List<String> carrierStringRecords, String carrierService);

  @Query("select smc.id,smc.shipping_method_id,smc.carrier_name,smc.carrier_service,smc.custom_id,smc.default_shipping_method,smc.description,smc.fulfillment_types,smc.max_days_to_deliver,smc.min_days_to_deliver,smc.name,smc.prices,smc.tax_class_id,smcs.carrier_string,c.channel_name as channel_list,p.product_type_name as product_type_list" +
      " from site_shipping_method_configuration smc " +
      "         inner join shipping_method_channel_mapping smcm on smc.id = smcm.shipping_method_id inner join channel c on (c.channel_id = smcm.channel_id and c.channel_name ILIKE :channels) or :channels is null " +
      "         inner join shipping_method_product_type_mapping smp on smc.id = smp.shipping_method_id inner join product_type p on (p.product_type_id = smp.product_type_id and p.product_type_name in (:productTypes)) or length(concat(:productTypes)) = 0 " +
      "         inner join shipping_method_carrier_string smcs on (smc.id = smcs.shipping_method_id and smcs.carrier_string in (:carrierStringRecords)) or length(concat(:carrierStringRecords)) = 0 " +
      "where smc.site_id = :siteId and smc.enabled = true and :fulfillmentType = ANY(smc.fulfillment_types) " +
      "and (smc.carrier_service in (:carrierServices) or length(concat(:carrierServices)) = 0)")
  Flux<ShippingMethods> findShippingMethodsByUniqueParams(Integer siteId, String channels, Set<String> productTypes, Set<String> carrierStringRecords, Set<String> carrierServices, String fulfillmentType);

  @Query("select smc.id,smc.shipping_method_id,smc.carrier_name,smc.carrier_service,smc.custom_id,smc.default_shipping_method,smc.description,smc.fulfillment_types,smc.max_days_to_deliver,smc.min_days_to_deliver,smc.name,smc.prices,smc.tax_class_id,smcs.carrier_string,c.channel_name as channel_list,p.product_type_name as product_type_list" +
      ",( select string_agg(t,',') from unnest(smc.availability_status) t(t) inner join unnest(v.availability_status) s(t) using (t) ) as availability_status_list " +
      "  from site_shipping_method_configuration smc " +
      "         inner join shipping_method_channel_mapping smcm on smc.id = smcm.shipping_method_id inner join channel c on (c.channel_id = smcm.channel_id and c.channel_name ILIKE :channels) or :channels is null " +
      "         inner join (values (:availabilityStatus::varchar[])) v(availability_status) on smc.availability_status && v.availability_status or ( EXISTS (SELECT 1 FROM unnest(v.availability_status) x WHERE x IS NULL) and smc.availability_status is null) " +
      "         inner join shipping_method_product_type_mapping smp on smc.id = smp.shipping_method_id inner join product_type p on (p.product_type_id = smp.product_type_id and p.product_type_name in (:productTypes)) or length(concat(:productTypes)) = 0 " +
      "         inner join shipping_method_carrier_string smcs on (smc.id = smcs.shipping_method_id and smcs.carrier_string in (:carrierStringRecords)) or length(concat(:carrierStringRecords)) = 0 " +
      "where smc.site_id = :siteId and smc.enabled = true and :fulfillmentType = ANY(smc.fulfillment_types) " +
      "and (smc.carrier_service in (:carrierServices) or length(concat(:carrierServices)) = 0)")
  Flux<ShippingMethods> findShippingByUniqueParamsWithAvailabilityStatus(Integer siteId, String channels, Set<String> productTypes,
      Set<String> carrierStringRecords, Set<String> carrierServices, String fulfillmentType,
      String[] availabilityStatus);

  @Query("select count(smc.id) > 0" +
      " from site_shipping_method_configuration smc " +
      "         inner join shipping_method_channel_mapping smcm on smc.id = smcm.shipping_method_id inner join channel c on (c.channel_id = smcm.channel_id and c.channel_name in (:channels)) or length(concat(:channels)) = 0 " +
      "         inner join shipping_method_product_type_mapping smp on smc.id = smp.shipping_method_id inner join product_type p on (p.product_type_id = smp.product_type_id and p.product_type_name in (:productTypes)) or length(concat(:productTypes)) = 0 " +
      "         inner join shipping_method_carrier_string smcs on (smc.id = smcs.shipping_method_id and smcs.carrier_string in (:carrierStringRecords)) or length(concat(:carrierStringRecords)) = 0 " +
      "where smc.site_id = :siteId " +
      "and (smc.carrier_service =:carrierService or :carrierService is null) " +
      "and smc.id <> :shippingMethodId and smc.enabled=true")
  Mono<Boolean> existsShippingMethodsByUniqueParamsAndShippingMethodId(
      Integer siteId,
      Set<String> channels,
      Set<String> productTypes,
      Set<String> carrierStringRecords,
      String carrierService,
      Integer shippingMethodId);

  @Query("select smcs.shipping_method_id, smcs.carrier_string_id, smcs.carrier_string from shipping_method_carrier_string smcs where smcs.shipping_method_id=:shippingId and smcs.carrier_string in (:carrierStrings)")
  Flux<ShippingMethodCarrierStringRecord> findDuplicateCarrierStringsByCarrierStrings(List<String> carrierStrings, Integer shippingMethodId);

  Mono<ShippingMethods> findByShippingMethodId(UUID id);

  @Query("select smc.shipping_method_id, smc.prices, si.price_config from site_shipping_method_configuration smc inner join site_id si on smc.site_id = si.id where smc.shipping_method_id in (:shippingMethodId)")
  Flux<ShippingPrice> findPriceByShippingMethodIds(List<UUID> shippingMethodId);

  @Query("select smc.id,smr.rule_id,smr.allow,smr.operator,smr.type,smr.dependent_rule_id,string_agg(distinct smrd.data_value,',') as rule_data "
      + " from site_shipping_method_configuration smc "
      + " INNER JOIN shipping_method_rule smr on smc.id = smr.shipping_method_id "
      + " INNER JOIN shipping_method_rule_data smrd on smr.id = smrd.rule_id where smc.shipping_method_id in (:shippingMethodId) and smr.enabled='true' "
      + " group by smc.id,smr.rule_id,smr.allow,smr.operator,smr.type,smr.dependent_rule_id")
  Flux<ShippingMethodsRulesData> findRuleDataByShippingMethodId(List<UUID> shippingMethodId);

  @Query("select smc.id,smc.shipping_method_id,si.name as site_name,smc.carrier_name,smc.carrier_service,smc.carrier_string "
      + " ,smc.created_by,smc.created_date,smc.custom_id,smc.default_shipping_method,smc.description, "
      + " smc.enabled,smc.fulfillment_types,smc.max_days_to_deliver,smc.min_days_to_deliver, "
      + " smc.modified_by,smc.modified_date,smc.\"name\",smc.\"position\",smc.prices,smc.tax_class_id "
      + " ,string_agg(distinct c.channel_name,',') as channel_list "
      + " ,string_agg(distinct p.product_type_name,',') as product_type_list "
      + " from site_shipping_method_configuration smc "
      + " inner join site_id si on smc.site_id=si.id and si.name = :name "
      + " inner join shipping_method_channel_mapping smcm on smc.id = smcm.shipping_method_id "
      + " inner join channel c on c.channel_id = smcm.channel_id "
      + " inner join shipping_method_product_type_mapping smp on smc.id = smp.shipping_method_id "
      + " inner join product_type p on p.product_type_id = smp.product_type_id "
      + " group by smc.id,smc.shipping_method_id,si.name,smc.carrier_name,smc.carrier_service,smc.carrier_string "
      + " ,smc.created_by,smc.created_date,smc.custom_id,smc.default_shipping_method,smc.description, "
      + " smc.enabled,smc.fulfillment_types,smc.max_days_to_deliver,smc.min_days_to_deliver, "
      + " smc.modified_by,smc.modified_date,smc.\"name\",smc.\"position\",smc.prices,smc.tax_class_id "
      + " order by smc.position")
  Flux<ShippingMethodsRuleResponse> findShippingMethodsBySiteId(String siteId);

  @Query("select smc.id,smc.shipping_method_id,smc.carrier_name,smc.carrier_service,smc.custom_id,smc.default_shipping_method,smc.description,smc.fulfillment_types,smc.max_days_to_deliver,smc.min_days_to_deliver,smc.name,smc.prices,smc.tax_class_id,smcs.carrier_string "
             + " ,string_agg(distinct c.channel_name,',') as channel_list "
             + " ,string_agg(distinct p.product_type_name,',') as product_type_list "
             + " from site_shipping_method_configuration smc "
             + " inner join shipping_method_channel_mapping smcm on smc.id = smcm.shipping_method_id "
             + " inner join channel c on c.channel_id = smcm.channel_id "
             + " inner join shipping_method_product_type_mapping smp on smc.id = smp.shipping_method_id "
             + " inner join product_type p on p.product_type_id = smp.product_type_id "
             + " inner join shipping_method_carrier_string smcs on smc.id = smcs.shipping_method_id "
             + " where smc.site_id = :siteId and smc.enabled = true and :fulfillmentType = ANY(smc.fulfillment_types) "
             + " group by smc.id,smc.shipping_method_id,smc.carrier_name,smc.carrier_service,smc.custom_id,smc.default_shipping_method,smc.description,smc.fulfillment_types,smc.max_days_to_deliver,smc.min_days_to_deliver,smc.name,smc.prices,smc.tax_class_id,smcs.carrier_string")
  Mono<ShippingMethods> findBySiteIdAndFulfillmentType(Integer siteId, String fulfillmentType);

  @Query("select smc.id,smc.shipping_method_id,smc.carrier_name,smc.carrier_service,smc.custom_id,smc.default_shipping_method,smc.description,smc.fulfillment_types,smc.max_days_to_deliver,smc.min_days_to_deliver,smc.name,smc.prices,smc.tax_class_id,smcs.carrier_string\r\n"
      + " ,string_agg(distinct c.channel_name,',') as channel_list\r\n"
      + " ,string_agg(distinct p.product_type_name,',') as product_type_list\r\n"
      + " from site_shipping_method_configuration smc\r\n"
      + " inner join shipping_method_channel_mapping smcm on smc.id = smcm.shipping_method_id\r\n"
      + " inner join channel c on c.channel_id = smcm.channel_id\r\n"
      + " inner join shipping_method_product_type_mapping smp on smc.id = smp.shipping_method_id\r\n"
      + " inner join product_type p on p.product_type_id = smp.product_type_id\r\n"
      + " inner join shipping_method_carrier_string smcs on smc.id = smcs.shipping_method_id and smcs.carrier_string in (:carrierStringRecords)"
      + " where smc.site_id = :siteId  and smc.enabled = true and :fulfillmentType = ANY(smc.fulfillment_types)\r\n"
      + " group by smc.id,smc.shipping_method_id,smc.carrier_name,smc.carrier_service,smc.custom_id,smc.default_shipping_method,smc.description,smc.fulfillment_types,smc.max_days_to_deliver,smc.min_days_to_deliver,smc.name,smc.prices,smc.tax_class_id,smcs.carrier_string")
  Flux<ShippingMethods> findByCarriersAndFulfillmentType(Integer siteId, String fulfillmentType, Collection<String> carrierStrings);

  @Query("select smc.id,smc.shipping_method_id,smc.carrier_name,smc.carrier_service,smc.custom_id,smc.default_shipping_method,smc.description,smc.fulfillment_types,smc.max_days_to_deliver,smc.min_days_to_deliver,smc.name,smc.prices,smc.tax_class_id,smcs.carrier_string,smcs.carrier_string,c.channel_name as channel_list,p.product_type_name as product_type_list  "
      + " from site_shipping_method_configuration smc"
      + " inner join shipping_method_channel_mapping smcm on smc.id = smcm.shipping_method_id inner join channel c on c.channel_id = smcm.channel_id and c.channel_name ILIKE :channel "
      + " inner join shipping_method_product_type_mapping smp on smc.id = smp.shipping_method_id inner join product_type p on p.product_type_id = smp.product_type_id and p.product_type_name in (:productTypes) "
      + " inner join shipping_method_carrier_string smcs on smc.id = smcs.shipping_method_id"
      + " where smc.site_id = :siteId and smc.enabled = true "
      + " and smc.default_shipping_method = true and :fulfillmentType = ANY(fulfillment_types)"
  )
  Flux<ShippingMethods> findShippingMethodsByDefaultOptions (Integer siteId, String fulfillmentType, String channel, Set<String> productTypes);

  @Query("select smc.id,smc.shipping_method_id,smc.carrier_name,smc.carrier_service,smc.custom_id,smc.default_shipping_method,smc.description,smc.fulfillment_types,smc.max_days_to_deliver,smc.min_days_to_deliver,smc.name,smc.prices,smc.tax_class_id,smcs.carrier_string"
   + " ,string_agg(distinct c.channel_name,',') as channel_list"
   + " ,string_agg(distinct p.product_type_name,',') as product_type_list"
   + " from site_shipping_method_configuration smc"
   + " inner join shipping_method_channel_mapping smcm on smc.id = smcm.shipping_method_id inner join channel c on c.channel_id = smcm.channel_id and c.channel_name in (:channels)"
   + " inner join shipping_method_product_type_mapping smp on smc.id = smp.shipping_method_id"
   + " inner join product_type p on p.product_type_id = smp.product_type_id"
   + " inner join shipping_method_carrier_string smcs on smc.id = smcs.shipping_method_id and smcs.carrier_string in (:carrierStrings)"
   + " where smc.site_id = :siteId  and smc.enabled = true and :fulfillmentType = ANY(smc.fulfillment_types)"
   + " and carrier_service in (:carrierService)"
   + " group by smc.id,smc.shipping_method_id,smc.carrier_name,smc.carrier_service,smc.custom_id,smc.default_shipping_method,smc.description,smc.fulfillment_types,smc.max_days_to_deliver,smc.min_days_to_deliver,smc.name,smc.prices,smc.tax_class_id,smcs.carrier_string")
  Flux<ShippingMethods> findByCarrierStringServiceAndChannels(Integer siteId, String fulfillmentType,
      Set<String> carrierStrings, Set<String> channels, Set<String> carrierService);

  Mono<Integer> deleteByCarrierString(UUID carrierStringId);
  
  @Modifying
  @Query("update site_shipping_method_configuration set default_shipping_method = false where site_id = :siteId and enabled = true and default_shipping_method = true")
  Mono<Integer> markShippingMethodsDefaultFalseBySiteId(Integer siteId);
}
