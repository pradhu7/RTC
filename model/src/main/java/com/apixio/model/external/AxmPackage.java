package com.apixio.model.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;

public class AxmPackage {
  private String version;
  private AxmPatient patient;

  public void setPatient(AxmPatient patient) {
    this.patient = patient;
  }

  public AxmPatient getPatient() {
    return patient;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getVersion() {
    return version;
  }

  @Override
  public String toString()
  {
      try {
          return AxmUtils.toJson(this);
      }
      catch (JsonProcessingException e)
      {
          throw new RuntimeException("Failed to JSON-serialize AXM package");
      }
  }
}
