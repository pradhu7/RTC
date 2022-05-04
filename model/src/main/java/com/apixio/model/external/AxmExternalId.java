package com.apixio.model.external;

import java.util.*;

public class AxmExternalId extends AxmRow {

  private String id;
  private String assignAuthority;

  public AxmExternalId(){}

  public AxmExternalId(String id, String assignAuthority)
  {
    this.id = id;
    this.assignAuthority = assignAuthority;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getId() {
    return id;
  }

  public void setAssignAuthority(String assignAuthority) {
    this.assignAuthority = assignAuthority;
  }

  public String getAssignAuthority() {
    return assignAuthority;
  }

  @Override
  public String toString()
  {
    return id + "^^" + assignAuthority;
  }

  @Override
  public int hashCode()
  {
      return Objects.hash(id, assignAuthority, metadata, source, editType);
  }

  @Override
  public boolean equals(final Object obj)
  {
      if (obj == null) return false;
      if (obj == this) return true;
      if (this.getClass() != obj.getClass()) return false;

      final AxmExternalId that = (AxmExternalId) obj;
      return Objects.equals(this.id, that.id)
          && Objects.equals(this.assignAuthority, that.assignAuthority)
          && Objects.equals(this.metadata, that.metadata)
          && Objects.equals(this.source, that.source)
          && Objects.equals(this.editType, that.editType);
  }
}
