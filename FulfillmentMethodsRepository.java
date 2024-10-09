package com.domainname.next.shippingapi.repository;


import com.domainname.next.shippingapi.entity.FulfillmentMethods;
import com.domainname.next.shippingapi.entity.FulfillmentOption;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;


public interface FulfillmentMethodsRepository extends ReactiveCrudRepository<FulfillmentMethods, Integer> {

  @Query("select sfc.* from site_fulfillment_configuration sfc " +
      "inner join site_id si on sfc.site_id = si.id " +
      "where si.name=:name order by sfc.position")
  Flux<FulfillmentMethods> findBySiteIdName(String name);


  Mono<FulfillmentMethods> getByFulfillmentId(UUID fulfillmentId);

  @Query("select fulfillment_type, name, description " +
      " FROM site_fulfillment_configuration " +
      " where site_id = :siteId " +
      "  and fulfillment_type in (:fulfillmentTypes)" +
      "  and enabled = true order by position")
  Flux<FulfillmentOption> findBySiteIdAndFulfilmentType(Integer siteId, List<String> fulfillmentTypes);
}
