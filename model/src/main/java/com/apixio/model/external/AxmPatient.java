package com.apixio.model.external;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.*;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class AxmPatient {
  private AxmDemographics demographics;

  /**
   * In fact we are interested in a set of external ids rather than a list.
   * Due to deduplication logic in loader validator this list will always contain unique values.
   * In AXMParser this list will be forced into a Set of APO ExternalID objects.
   * In order to not duplicate equality comparison logic between AxmExternalId and ExternalID
   * and given that AXM model will completely go away as part of loading population documents functionality
   * this is still a list rather than a set.
   */
  private List<AxmExternalId> externalIds = new ArrayList<>();

  private List<AxmDocument> documents = new ArrayList<>();
  private List<AxmProblem> problems = new ArrayList<>();
  private List<AxmEncounter> encounters = new ArrayList<>();
  private List<AxmProvider> providers = new ArrayList<>();
  private List<AxmCoverage> coverages = new ArrayList<>();
  private List<AxmProcedure> procedures = new ArrayList<>();
  private List<AxmPrescription> prescriptions = new ArrayList<>();
  private List<AxmBiometricValue> biometricValues = new ArrayList<>();
  private List<AxmSocialHistory> socialHistories = new ArrayList<>();

  private List<AxmLabResult> labResults = new ArrayList<>();


  public AxmDemographics getDemographics() {
    return demographics;
  }

  public void setDemographics(AxmDemographics demographics) {
    this.demographics = demographics;
  }

  public void addExternalId(AxmExternalId externalId) {
    externalIds.add(externalId);
  }

  public Collection<AxmExternalId> getExternalIds() {
    return externalIds;
  }

  public void addDocument(AxmDocument document) {
    documents.add(document);
  }

  public Collection<AxmDocument> getDocuments() {
    return documents;
  }

  public void addProblem(AxmProblem problem) {
    problems.add(problem);
  }

  public Collection<AxmProblem> getProblems() {
    return problems;
  }

  public void addEncounter(AxmEncounter encounter) {
    encounters.add(encounter);
  }

  public Collection<AxmEncounter> getEncounters() {
    return encounters;
  }

  public void addProvider(AxmProvider provider) {
    providers.add(provider);
  }

  public Collection<AxmProvider> getProviders() {
    return providers;
  }

  public void addCoverage(AxmCoverage coverage) {
    coverages.add(coverage);
  }

  public Collection<AxmCoverage> getCoverages() {
    return coverages;
  }

  public void addProcedure(AxmProcedure procedure) {
    procedures.add(procedure);
  }

  public Collection<AxmProcedure> getProcedures() {
    return procedures;
  }

  public List<AxmPrescription> getPrescriptions() { return prescriptions; }

  public void addPrescription(AxmPrescription prescription) { prescriptions.add(prescription); }

  public List<AxmBiometricValue> getBiometricValues() { return biometricValues; }

  public void addBiometricValue(AxmBiometricValue biometricValue) { biometricValues.add(biometricValue); }

  public List<AxmSocialHistory> getSocialHistories() { return socialHistories; }

  public void addSocialHistory(AxmSocialHistory socialHistory) { socialHistories.add(socialHistory); }

  public List<AxmLabResult> getLabResults() { return labResults; }

  public void addLabResult(AxmLabResult labResult) { labResults.add(labResult); }

  @Override
  public int hashCode()
  {
      return Objects.hash(demographics, externalIds, documents, problems, encounters, providers, coverages, procedures,
              prescriptions, biometricValues, socialHistories, labResults);
  }

  @Override
  public boolean equals(final Object obj)
  {
      if (obj == null) return false;
      if (obj == this) return true;
      if (this.getClass() != obj.getClass()) return false;

      final AxmPatient that = (AxmPatient) obj;
      return Objects.equals(this.demographics, that.demographics)
          && Objects.equals(this.externalIds, that.externalIds)
          && Objects.equals(this.documents, that.documents)
          && Objects.equals(this.problems, that.problems)
          && Objects.equals(this.encounters, that.encounters)
          && Objects.equals(this.providers, that.providers)
          && Objects.equals(this.coverages, that.coverages)
          && Objects.equals(this.procedures, that.procedures)
          && Objects.equals(this.prescriptions, that.prescriptions)
          && Objects.equals(this.biometricValues, that.biometricValues)
          && Objects.equals(this.socialHistories, that.socialHistories)
          && Objects.equals(this.labResults, that.labResults);
  }

  @Override
  public String toString()
  {
      try {
          return AxmUtils.toJson(this);
      }
      catch (JsonProcessingException e)
      {
          throw new RuntimeException("Failed to JSON-serialize AXM patient");
      }
  }

  @JsonIgnore
  public boolean isEmptyAxm()
  {
    return  (demographics == null || demographics.isEmpty())
            && (externalIds.isEmpty() || externalIds.size() == 1)
            && documents.isEmpty()
            && problems.isEmpty()
            && encounters.isEmpty()
            && problems.isEmpty()
            && coverages.isEmpty()
            && procedures.isEmpty()
            && prescriptions.isEmpty()
            && biometricValues.isEmpty()
            && socialHistories.isEmpty()
            && labResults.isEmpty()
            && providers.isEmpty();
  }
}
