package com.apixio.model.external;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.*;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class AxmProvider extends AxmRow {
    private AxmExternalId originalId;
    private Set<AxmExternalId> alternateIds = new HashSet<>();
    private List<String> givenNames = new ArrayList<>();
    private List<String> familyNames = new ArrayList<>();
    private List<String> prefixes = new ArrayList<>();
    private List<String> suffixes = new ArrayList<>();
    private String title;
    private String orgName;
    private List<String> emails = new ArrayList<>();
    private AxmPhoneNumber phoneNumber;

    public AxmExternalId getOriginalId() {
        return originalId;
    }

    public void setOriginalId(AxmExternalId originalId) {
        this.originalId = originalId;
    }

    public Iterable<AxmExternalId> getAlternateIds() {
        return alternateIds;
    }

    public void addAlternateId(AxmExternalId id) {
        alternateIds.add(id);
    }

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

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public String getOrgName() {
        return orgName;
    }

    public void setOrgName(String orgName) {
        this.orgName = orgName;
    }

    public void addEmail(String email) {
        emails.add(email);
    }

    public Iterable<String> getEmails() {
        return emails;
    }

    public void setPhoneNumber(AxmPhoneNumber phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public AxmPhoneNumber getPhoneNumber() {
        return phoneNumber;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(originalId, alternateIds, givenNames, familyNames, prefixes, suffixes, title, orgName, emails, phoneNumber, metadata, source, editType);
    }

    @Override
    public boolean equals(final Object obj)
    {
        if (obj == null) return false;
        if (obj == this) return true;
        if (this.getClass() != obj.getClass()) return false;

        final AxmProvider that = (AxmProvider) obj;
        return Objects.equals(this.originalId, that.originalId)
            && Objects.equals(this.alternateIds, that.alternateIds)
            && Objects.equals(this.givenNames, that.givenNames)
            && Objects.equals(this.familyNames, that.familyNames)
            && Objects.equals(this.prefixes, that.prefixes)
            && Objects.equals(this.suffixes, that.suffixes)
            && Objects.equals(this.title, that.title)
            && Objects.equals(this.orgName, that.orgName)
            && Objects.equals(this.emails, that.emails)
            && Objects.equals(this.phoneNumber, that.phoneNumber)
            && Objects.equals(this.metadata, that.metadata)
            && Objects.equals(this.source, that.source)
            && Objects.equals(this.editType, that.editType);
    }
}
