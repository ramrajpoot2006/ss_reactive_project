package com.domainname.next.shippingapi.repository;

import com.domainname.next.shippingapi.entity.SiteId;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface SiteIdRepository extends ReactiveCrudRepository<SiteId, Integer>{

    Mono<SiteId> findById(Integer SiteId);

    Mono<SiteId> findByName(String name);
}
