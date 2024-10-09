package com.domainname.next.shippingapi.client;

import com.domainname.next.shippingapi.client.request.DeliveryPromiseRequest;
import com.domainname.next.shippingapi.client.response.DeliveryPromiseResponse;
import com.domainname.next.shippingapi.interfaces.TargetService;

import reactor.core.publisher.Mono;

public interface DeliveryPromiseClientTargetService extends TargetService {
  Mono<DeliveryPromiseResponse> getDeliveryPromise(DeliveryPromiseRequest deliveryPromiseRequest);
}
