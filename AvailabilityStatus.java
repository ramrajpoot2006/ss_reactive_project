package com.domainname.next.shippingapi.enums;

import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum AvailabilityStatus {

  BACKORDER("BACKORDER");

  private final String value;

  @JsonCreator
  public static AvailabilityStatus fromValue(String text) {
    return Stream.of(AvailabilityStatus.values()).filter(targetEnum -> targetEnum.value.equals(text)).findFirst()
        .orElse(null);
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return value;
  }

}
