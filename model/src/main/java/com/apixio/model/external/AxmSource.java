package com.apixio.model.external;

import java.util.*;

import org.joda.time.LocalDate;

public class AxmSource {
  private String system;
  private String type;
  private LocalDate date;

  /**
   * dci stands for Data Completeness Interval
   */
  private LocalDate dciStartDate;
  private LocalDate dciEndDate;

  public String getSystem() {
    return system;
  }

  public void setSystem(String system) {
    this.system = system;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public LocalDate getDate() {
    return date;
  }

  public void setDate(LocalDate date) {
    this.date = date;
  }

  public LocalDate getDciStartDate() {
    return dciStartDate;
  }

  public void setDciStartDate(LocalDate date) {
    this.dciStartDate = date;
  }

  public LocalDate getDciEndDate() {
    return dciEndDate;
  }

  public void setDciEndDate(LocalDate date) {
    this.dciEndDate = date;
  }

  @Override
  public int hashCode()
  {
      return Objects.hash(system, type, date, dciStartDate, dciEndDate);
  }

  @Override
  public boolean equals(final Object obj)
  {
      if (obj == null) return false;
      if (obj == this) return true;
      if (this.getClass() != obj.getClass()) return false;

      final AxmSource that = (AxmSource) obj;
      return Objects.equals(this.system, that.system)
          && Objects.equals(this.type, that.type)
          && Objects.equals(this.date, that.date)
          && Objects.equals(this.dciStartDate, that.dciStartDate)
          && Objects.equals(this.dciEndDate, that.dciEndDate);
  }
}
