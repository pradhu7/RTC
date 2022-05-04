package com.apixio.model.external;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.joda.time.LocalDate;

import java.net.URI;
import java.util.*;


@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class AxmDocument extends AxmRow {
  private AxmExternalId originalId;
  private String title;
  private LocalDate date;
  private AxmExternalId encounterId;
  private List<AxmProviderInRole> providerInRoles = new ArrayList<>();
  private URI uri;
  private String content;
  private String mimeType;
  private AxmCodeOrName codeOrName;
  private String documentUuid;

  public AxmExternalId getOriginalId() {
    return originalId;
  }

  public void setOriginalId(AxmExternalId originalId) {
    this.originalId = originalId;
  }

  public String getDocumentUuid() { return documentUuid; }

  public void setDocumentUuid(String documentUuid) { this.documentUuid = documentUuid; }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public LocalDate getDate() {
    return date;
  }

  public void setDate(LocalDate date) {
    this.date = date;
  }

  public AxmExternalId getEncounterId() {
    return encounterId;
  }

  public void setEncounterId(AxmExternalId encounterId) {
    this.encounterId = encounterId;
  }

  public void addProviderInRole(AxmProviderInRole providerInRole) {
    providerInRoles.add(providerInRole);
  }

  public Iterable<AxmProviderInRole> getProviderInRoles() {
    return providerInRoles;
  }

  public URI getUri() {
    return uri;
  }

  public void setUri(URI uri) {
    this.uri = uri;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public String getMimeType() {
    return mimeType;
  }

  public void setMimeType(String mimeType) {
    this.mimeType = mimeType;
  }

  public AxmCodeOrName getCodeOrName() {
    return codeOrName;
  }

  public void setCodeOrName(AxmCodeOrName codeOrName) {
    this.codeOrName = codeOrName;
  }

  @Override
  public int hashCode()
  {
      return Objects.hash(originalId, documentUuid, title, date, encounterId, providerInRoles, uri, content, mimeType, codeOrName, metadata, source, editType);
  }

  @Override
  public boolean equals(final Object obj)
  {
      if (obj == null) return false;
      if (obj == this) return true;
      if (this.getClass() != obj.getClass()) return false;

      final AxmDocument that = (AxmDocument) obj;
      return Objects.equals(this.originalId, that.originalId)
          && Objects.equals(this.documentUuid, that.documentUuid)
          && Objects.equals(this.title, that.title)
          && Objects.equals(this.date, that.date)
          && Objects.equals(this.encounterId, that.encounterId)
          && Objects.equals(this.providerInRoles, that.providerInRoles)
          && Objects.equals(this.uri, that.uri)
          && Objects.equals(this.content, that.content)
          && Objects.equals(this.mimeType, that.mimeType)
          && Objects.equals(this.codeOrName, that.codeOrName)
          && Objects.equals(this.metadata, that.metadata)
          && Objects.equals(this.source, that.source)
          && Objects.equals(this.editType, that.editType);
  }
}
