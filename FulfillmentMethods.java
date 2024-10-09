package com.domainname.next.shippingapi.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import io.r2dbc.postgresql.codec.Json;
import lombok.Builder;
import lombok.Getter;

@Builder(toBuilder = true)
@Getter
@Table("site_fulfillment_configuration")
public class FulfillmentMethods {

  @Id
  @Column("fulfillment_id")
  private UUID fulfillmentId;
  @Column("site_id")
  private Integer siteId;
  @Column("fulfillment_type")
  private String fulfillmentType;
  private Boolean enabled;
  private Json name;
  private Json description;
  private Integer position;
  @Column("created_by")
  private String createdBy;
  @Column("created_date")
  private LocalDateTime createdDate;
  @Column("modified_by")
  private String modifiedBy;
  @Column("modified_date")
  private LocalDateTime modifiedDate;

}
