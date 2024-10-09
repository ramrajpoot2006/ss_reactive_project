package com.domainname.next.shippingapi.repository;


import com.domainname.next.shippingapi.entity.Channel;
import com.domainname.next.shippingapi.entity.ProductType;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.util.List;


public interface ChannelRepository extends ReactiveCrudRepository<Channel, Integer> {

  @Query("select channel.* from channel where channel_id in (" +
            "select channel_id from shipping_method_channel_mapping  " +
            "where shipping_method_id = :shippingId)")
  Flux<Channel> findChannelByShippingId(Integer shippingId);

  Flux<Channel> findByChannelNameIn(List<String> channelNames);

}
