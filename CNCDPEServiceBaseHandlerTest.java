package com.domainname.next.shippingapi.handler;

import static com.domainname.next.shippingapi.constant.ErrorConstants.NOT_FOUND_CODE;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.HttpClientErrorException;

import com.domainname.next.shippingapi.TestHelper;
import com.domainname.next.shippingapi.client.SSMStoreClient;
import com.domainname.next.shippingapi.client.response.cnc.StoreServiceResponse;
import com.domainname.next.shippingapi.converter.cnc.StoreServiceRequestConverter;
import com.domainname.next.shippingapi.converter.cnc.StoreServiceResponseConverter;
import com.domainname.next.shippingapi.exception.NotFoundException;
import com.domainname.next.shippingapi.handler.cnc.CNCServiceBaseHandler;
import com.domainname.next.shippingapi.util.TargetServiceManager;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(SpringExtension.class)
class CNCDPEServiceBaseHandlerTest extends TestHelper {
  
  @InjectMocks
  CNCServiceBaseHandler cncDPEServiceBaseHandler;
  
  @Mock
  StoreServiceRequestConverter storeServiceRequestConverter;
  
  @Mock
  SSMStoreClient ssmStoreClient;
  
  @Mock
  StoreServiceResponseConverter storeServiceResponseConverter;

  @Mock
  TargetServiceManager targetServiceManager;
  

  @Test
  void testGetSSMStoreWhenStoreIdPresent() {
    Mockito.when(ssmStoreClient.getStoreById(Mockito.any(), Mockito.any())).thenReturn(Mono.just(buildSsmStoreAPIResponse()));
    Mockito.when(targetServiceManager.getClientForSiteId(Mockito.any(), Mockito.any())).thenReturn(ssmStoreClient);
    Mockito.when(storeServiceResponseConverter.prepareStoreServiceResponse(Mockito.any())).thenReturn(Mono.just(buildSsmStoreResponse()));
    Mockito.when(storeServiceResponseConverter.getFilteredStoreServiceResponse(Mockito.any())).thenReturn(Mono.just(buildSsmStoreResponse()));
    Mono<StoreServiceResponse> ssmStoreResponse =
        cncDPEServiceBaseHandler.getStores(buildShippingOptionsPostRequestSsmWithStoreId(), buildSiteId(), buildGeoCodingResponse());
    StepVerifier.create(ssmStoreResponse).thenConsumeWhile(ssmStoreRes -> {
      Assertions.assertEquals("9990000553", ssmStoreRes.getContent().get(0).getStore().getSsmId());
      Assertions.assertEquals(206, ssmStoreRes.getContent().get(0).getDistanceMeters());
      Assertions.assertEquals(31.06, ssmStoreRes.getContent().get(0).getDistanceMiles());
      return true;
    }).verifyComplete();
  }
  
  @Test
  void testGetSSMStoreWhenStoreIdNotPresent() {
    Mockito.when(storeServiceRequestConverter.apply(Mockito.any(), Mockito.any())).thenReturn(Mono.just(buildStoreClientRequest()));
    Mockito.when(targetServiceManager.getClientForSiteId(Mockito.any(), Mockito.any())).thenReturn(ssmStoreClient);
    Mockito.when(ssmStoreClient.getStores(Mockito.any(), Mockito.any())).thenReturn(Mono.just(buildSsmStoreResponse()));
    Mockito.when(storeServiceResponseConverter.getFilteredStoreServiceResponse(Mockito.any())).thenReturn(Mono.just(buildSsmStoreResponse()));
    Mono<StoreServiceResponse> ssmStoreResponse =
        cncDPEServiceBaseHandler.getStores(buildShippingOptionsPostRequest(), buildSiteId(), buildGeoCodingResponse());
    StepVerifier.create(ssmStoreResponse).thenConsumeWhile(ssmStoreRes -> {
      Assertions.assertEquals("9990000553", ssmStoreRes.getContent().get(0).getStore().getSsmId());
      Assertions.assertEquals(206, ssmStoreRes.getContent().get(0).getDistanceMeters());
      Assertions.assertEquals(31.06, ssmStoreRes.getContent().get(0).getDistanceMiles());
      return true;
    }).verifyComplete(); 
  }

  @Test
  void testGetSSMStoreWhenStoreIdIsBlank() {
    Mockito.when(storeServiceRequestConverter.apply(Mockito.any(), Mockito.any())).thenReturn(Mono.just(buildStoreClientRequest()));
    Mockito.when(targetServiceManager.getClientForSiteId(Mockito.any(), Mockito.any())).thenReturn(ssmStoreClient);
    Mockito.when(ssmStoreClient.getStores(Mockito.any(), Mockito.any())).thenReturn(Mono.just(buildSsmStoreResponse()));
    Mockito.when(storeServiceResponseConverter.getFilteredStoreServiceResponse(Mockito.any())).thenReturn(Mono.just(buildSsmStoreResponse()));
    Mono<StoreServiceResponse> ssmStoreResponse =
        cncDPEServiceBaseHandler.getStores(buildShippingOptionsPostRequest().toBuilder().storeId("").build(), buildSiteId(), buildGeoCodingResponse());
    StepVerifier.create(ssmStoreResponse).thenConsumeWhile(ssmStoreRes -> {
      Assertions.assertEquals("9990000553", ssmStoreRes.getContent().get(0).getStore().getSsmId());
      Assertions.assertEquals(206, ssmStoreRes.getContent().get(0).getDistanceMeters());
      Assertions.assertEquals(31.06, ssmStoreRes.getContent().get(0).getDistanceMiles());
      return true;
    }).verifyComplete(); 
  }
  
  @Test
  void testGetSSMStoreWhenSSMAPIReturnEmptyStore() {
    Mockito.when(ssmStoreClient.getStoreById(Mockito.any(), Mockito.any())).thenReturn(Mono.just(buildSsmStoreAPIResponse()));
    Mockito.when(storeServiceResponseConverter.prepareStoreServiceResponse(Mockito.any())).thenReturn(Mono.just(buildSsmStoreResponseEmpty()));
    Mockito.when(storeServiceResponseConverter.getFilteredStoreServiceResponse(Mockito.any())).thenReturn(Mono.just(buildSsmStoreResponseEmpty()));
    Mono<StoreServiceResponse> ssmStoreResponse =
        cncDPEServiceBaseHandler.getStores(buildShippingOptionsPostRequestSsmWithStoreId(), buildSiteId(), buildGeoCodingResponse());
    StepVerifier.create(ssmStoreResponse)
    .consumeErrorWith(throwable -> new NotFoundException(new HttpClientErrorException(HttpStatus.NOT_FOUND), NOT_FOUND_CODE))
    .verify();
  }

}
