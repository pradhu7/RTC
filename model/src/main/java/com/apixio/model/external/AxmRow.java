package com.apixio.model.external;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public abstract class AxmRow {
  protected Map<String, String> metadata = new HashMap<>();
  protected AxmEditType editType;
  protected AxmSource source;

  public void setEditType(AxmEditType editType) {
    this.editType = editType;
  }

  public AxmEditType getEditType() {
    return editType;
  }

  public void setSource(AxmSource source) {
    this.source = source;
  }

  public AxmSource getSource() {
    return source;
  }

  public void addMetaTag(String key, String value) {
    metadata.put(key, value);
  }

  public String getMetaTag(String key) {
    return metadata.get(key);
  }

  @JsonIgnore
  public Iterable<String> getMetaKeys() {
    return metadata.keySet();
  }

  public Map<String, String> getMetaData() {
    return metadata;
  }

  @Override
  public int hashCode()
  {
      return Objects.hash(metadata, editType, source);
  }

  @Override
  public boolean equals(final Object obj)
  {
      if (obj == null) return false;
      if (obj == this) return true;
      if (this.getClass() != obj.getClass()) return false;

      final AxmRow that = (AxmRow) obj;
      return Objects.equals(this.metadata, that.metadata)
          && Objects.equals(this.editType, that.editType)
          && Objects.equals(this.source, that.source);
  }
}
