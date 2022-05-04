package com.apixio.model.external;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.joda.time.LocalDate;

import java.util.*;


@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class AxmDemographics extends AxmRow {
  private List<String> givenNames = new ArrayList<>();
  private List<String> familyNames = new ArrayList<>();
  private List<String> prefixes = new ArrayList<>();
  private List<String> suffixes = new ArrayList<>();
  private AxmGender gender;
  private LocalDate dateOfBirth;
  private LocalDate dateOfDeath;

  public void addGivenName(String givenName) {
    givenNames.add(givenName);
  }

  public Iterable<String> getGivenNames() {
    return givenNames;
  }

  public void addFamilyName(String familyName) {
    familyNames.add(familyName);
  }

  public Iterable<String> getFamilyNames() {
    return familyNames;
  }

  public void addPrefix(String prefix) {
    prefixes.add(prefix);
  }

  public Iterable<String> getPrefixes() {
    return prefixes;
  }

  public void addSuffix(String suffix) {
    suffixes.add(suffix);
  }

  public Iterable<String> getSuffixes() {
    return suffixes;
  }

  public void setGender(AxmGender gender) {
    this.gender = gender;
  }

  public AxmGender getGender() {
    return gender;
  }

  public void setDateOfBirth(LocalDate dateOfBirth) {
    this.dateOfBirth = dateOfBirth;
  }

  public LocalDate getDateOfBirth() {
    return dateOfBirth;
  }

  public void setDateOfDeath(LocalDate dateOfDeath) {
    this.dateOfDeath = dateOfDeath;
  }

  public LocalDate getDateOfDeath() {
    return dateOfDeath;
  }

  @Override
  public int hashCode()
  {
      return Objects.hash(givenNames, familyNames, prefixes, suffixes, gender, dateOfBirth, dateOfDeath, metadata, source, editType);
  }

  @Override
  public boolean equals(final Object obj)
  {
      if (obj == null) return false;
      if (obj == this) return true;
      if (this.getClass() != obj.getClass()) return false;

      final AxmDemographics that = (AxmDemographics) obj;
      return Objects.equals(this.givenNames, that.givenNames)
          && Objects.equals(this.familyNames, that.familyNames)
          && Objects.equals(this.prefixes, that.prefixes)
          && Objects.equals(this.suffixes, that.suffixes)
          && Objects.equals(this.gender, that.gender)
          && Objects.equals(this.dateOfBirth, that.dateOfBirth)
          && Objects.equals(this.dateOfDeath, that.dateOfDeath)
          && Objects.equals(this.metadata, that.metadata)
          && Objects.equals(this.source, that.source)
          && Objects.equals(this.editType, that.editType);
  }

  @JsonIgnore
  public boolean isEmpty()
  {
    return givenNames.isEmpty()
            && familyNames.isEmpty()
            && prefixes.isEmpty()
            && suffixes.isEmpty()
            && gender == null
            && dateOfBirth == null
            && dateOfDeath == null;

  }
}
