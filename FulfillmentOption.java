package com.domainname.next.shippingapi.entity;

import java.util.Map;

import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder(toBuilder = true)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table("site_fulfillment_configuration")
public class FulfillmentOption {
  
  @Column("fulfillment_type")
  private String fulfillmentType;
  private Map<String, String> name;
  private Map<String, String> description;

}
