package com.apixio.model.external;

import java.util.*;

public class AxmPhoneNumber {
  private String number;
  private AxmPhoneType phoneType;

  public void setNumber(String number) {
    this.number = number;
  }

  public String getNumber() {
    return number;
  }

  public void setPhoneType(AxmPhoneType phoneType) {
    this.phoneType = phoneType;
  }

  public AxmPhoneType getPhoneType() {
    return phoneType;
  }

  @Override
  public int hashCode()
  {
      return Objects.hash(number, phoneType);
  }

  @Override
  public boolean equals(final Object obj)
  {
      if (obj == null) return false;
      if (obj == this) return true;
      if (this.getClass() != obj.getClass()) return false;

      final AxmPhoneNumber that = (AxmPhoneNumber) obj;
      return Objects.equals(this.number, that.number)
          && Objects.equals(this.phoneType, that.phoneType);
  }
}
