package com.apixio.model.patient;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import com.apixio.model.Constants;
import com.apixio.model.WithMetadata;
import com.apixio.model.patient.event.Event;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The Patient is the container object (our primary object) within the apixio patient model that contains all
 * clincal information about a specific patient across multiple sources.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Patient extends WithMetadata {
    private List<Problem> problems;
    private List<Procedure> procedures;
    private List<Document> documents;
    private List<Prescription> prescriptions;
    private List<Administration> administrations;
    private List<LabResult> labs;
    private Collection<Apixion> apixions;
    private List<BiometricValue> biometricValues;
    private List<Allergy> allergies;
    private UUID patientId;
    private ExternalID primaryExternalID;
    private Set<ExternalID> externalIDs;
    private Demographics primaryDemographics;
    private List<Demographics> alternateDemographics;
    private ContactDetails primaryContactDetails;
    private List<ContactDetails> alternateContactDetails;
    private List<FamilyHistory> familyHistories;
    private List<SocialHistory> socialHistories;
    private Collection<Event> clinicalEvents;
    private List<Coverage> coverage;

    // an index to make lookup of encounter references faster, related to mechanics, not
    // semantics of the data model.
    private List<Encounter> encounters;
    private transient Map<UUID, Encounter> encounterMap = new HashMap<UUID, Encounter>();

    private List<Source> sources;
    private transient Map<UUID, Source> sourceMap = new HashMap<UUID, Source>();

    private List<ParsingDetail> parsingDetails;
    private transient Map<UUID, ParsingDetail> parsingDetailMap = new HashMap<UUID, ParsingDetail>();

    private List<ClinicalActor> clinicalActors;
    private transient Map<UUID, ClinicalActor> clinicalActorMap = new HashMap<UUID, ClinicalActor>();

    private List<CareSite> careSites;
    private transient Map<UUID, CareSite> careSiteMap = new HashMap<UUID, CareSite>();

    // custom methods.
    // the add x method are ways to add x to the patient. If any internal data structures need to be
    // updated for references to be correct, the add methods must automatically update them.
    public Patient addProblem(Problem p) {
        problems.add(p);
        return this;
    }

    public Patient addProcedure(Procedure p) {
        procedures.add(p);
        return this;
    }

    public Patient addDocument(Document p) {
        documents.add(p);
        return this;
    }

    public Patient addPrescription(Prescription p) {
        prescriptions.add(p);
        return this;
    }

    public Patient addAdministration(Administration a) {
        administrations.add(a);
        return this;
    }

    public Patient addLabResult(LabResult l) {
        labs.add(l);
        return this;
    }

    public Patient addBiometricValue(BiometricValue bv) {
        biometricValues.add(bv);
        return this;
    }

    public Patient addAllergy(Allergy a) {
        allergies.add(a);
        return this;
    }

    public Patient addExternalId(ExternalID externalId) {
        externalIDs.add(externalId);
        return this;
    }

    public Patient addAlternateDemographics(Demographics d) {
        alternateDemographics.add(d);
        return this;
    }

    public Patient addAlternateContactDetails(ContactDetails cd) {
        alternateContactDetails.add(cd);
        return this;
    }

    public Patient addFamilyHistory(FamilyHistory fh) {
        familyHistories.add(fh);
        return this;
    }

    public Patient addSocialHistory(SocialHistory sh) {
        socialHistories.add(sh);
        return this;
    }

    public Patient addEncounter(Encounter e) {
        getEncounterMap().put(e.getEncounterId(), e);
        encounters.add(e);
        return this;
    }

    public Patient addSource(Source s) {
        UUID sourceId = s.getSourceId();
        UUID internalId = s.getInternalUUID();
        if (!sourceMap.containsKey(sourceId) || !sourceMap.containsKey(internalId)) {
            sources.add(s);
        }

        sourceMap.put(sourceId, s);
        sourceMap.put(internalId, s);

        return this;
    }

    public Patient addParsingDetail(ParsingDetail parsingDetail) {
        getParsingDetailMap().put(parsingDetail.getParsingDetailsId(),parsingDetail);
        parsingDetails.add(parsingDetail);
        return this;
    }

    public Patient addClinicalActor(ClinicalActor actor) {
        getClinicalActorMap().put(actor.getClinicalActorId(), actor);
        clinicalActors.add(actor);
        return this;
    }

    public Patient addCareSite(CareSite cs) {
        getCareSiteMap().put(cs.getCareSiteId(),cs);
        careSites.add(cs);
        return this;
    }

    // custom methods.
    public Encounter getEncounterById(UUID encounterId) {
        return getEncounterMap().get(encounterId);
    }


    public Source getSourceById(UUID sourceId) {
        return getSourceMap().get(sourceId);
    }

    // custom methods.
    public ParsingDetail getParsingDetailById(UUID parsingDetailId) {
        return getParsingDetailMap().get(parsingDetailId);
    }

    // custom methods.
    public ClinicalActor getClinicalActorById(UUID clinicalActorId) {
        return getClinicalActorMap().get(clinicalActorId);
    }

    public CareSite getCareSiteById(UUID careSiteId) {
        Map<UUID, CareSite> careSiteMap = getCareSiteMap();
        return careSiteMap.get(careSiteId);
    }

    public Patient() {
        this.problems = new LinkedList<Problem>();
        this.encounters = new LinkedList<Encounter>();
        this.procedures = new LinkedList<Procedure>();
        this.documents = new LinkedList<Document>();
        this.apixions = new HashSet<Apixion>();
        this.labs = new LinkedList<LabResult>();
        this.prescriptions = new LinkedList<Prescription>();
        this.alternateContactDetails = new LinkedList<ContactDetails>();
        this.administrations = new LinkedList<Administration>();
        this.externalIDs = new HashSet<ExternalID>();
        this.allergies = new LinkedList<Allergy>();
        this.biometricValues = new LinkedList<BiometricValue>();
        this.alternateDemographics = new LinkedList<Demographics>();
        this.alternateContactDetails = new LinkedList<ContactDetails>();
        this.sources = new LinkedList<Source>();
        this.parsingDetails = new LinkedList<ParsingDetail>();
        this.clinicalActors = new LinkedList<ClinicalActor>();
        this.careSites = new LinkedList<CareSite>();
        this.familyHistories = new LinkedList<FamilyHistory>();
        this.socialHistories = new LinkedList<SocialHistory>();
        this.clinicalEvents = new LinkedList<Event>();
        this.coverage = new LinkedList<Coverage>();
    }

    /**
     *
     * @return    the UUID associated with this patient, which identifies this patient uniquely in our data store, managed and maintained
     * externally by an application layer solution.
     */
    public UUID getPatientId() {
        return patientId;
    }

    /**
     * set the patientId to the specified UUID value.
     * @param patientId    the new patientId
     */
    public void setPatientId(UUID patientId) {
        this.patientId = patientId;
    }

    /**
     * @param primaryExternalID the primaryExternalID to set
     */
    public void setPrimaryExternalID(ExternalID primaryExternalID) {
        this.primaryExternalID = primaryExternalID;
    }

    /**
     * @return the primaryExternalID
     */
    public ExternalID getPrimaryExternalID() {
        return primaryExternalID;
    }

    /**
     *
     * @return A list of problems from this patients problem list.
     */
    public Iterable<Problem> getProblems() {
        return problems;
    }

    /**
     *
     * @return A list of procedures performed on this patient.
     */
    public Iterable<Procedure> getProcedures() {
        return procedures;
    }

    /**
     * @return A list of text documents, scanned images, and potentially other multi-media documents associated with this patient
     */
    public Iterable<Document> getDocuments() {
        return documents;
    }

    public void setDocuments(List<Document> documents) {
        this.documents = documents;
    }

    public void setProblems(List<Problem> problems) {
        this.problems = problems;
    }

    public void setProcedures(List<Procedure> procedures) {
        this.procedures = procedures;
    }

    /**
     *
     * @return    A collection of all apixions that were found in the patient record (either computed, extracted or from other means)
     */
    public Iterable<Apixion> getApixions() {
        return apixions;
    }

    public void setApixions(Collection<Apixion> apixions) {
        this.apixions = apixions;
    }

    /**
     *
     * @return A list of all the lab results (analytes, specifically) that were obtained from this person
     */
    public Iterable<LabResult> getLabs() {
        return labs;
    }

    public void setLabs(List<LabResult> labs) {
        this.labs = labs;
    }

    /**
     * We define an encounter as any interaction between the patient and any clinical service provider (could be organization). Thus, we use encounters
     * to associate events that happened within a single conceptual encounter.
     * @return    A list of encounters associated with this patient.
     */
    public Iterable<Encounter> getEncounters() {
        return encounters;
    }

    public void setEncounters(List<Encounter> encounters) {
        this.encounters = encounters;
        encounterMap.clear();
        populateEncounters();
    }

    /**
     *
     * @return alternate contact details about the patient
     */
    public Iterable<ContactDetails> getAlternateContactDetails() {
        return alternateContactDetails;
    }

    public void setAlternateContactDetails(
            List<ContactDetails> supplementaryContactDetails) {
        this.alternateContactDetails = supplementaryContactDetails;
    }

    /**
     *
     * @return A list of all prescriptions associated with this patient.
     */
    public Iterable<Prescription> getPrescriptions() {
        return prescriptions;
    }

    public void setPrescriptions(List<Prescription> prescriptions) {
        this.prescriptions = prescriptions;
    }

    /**
     *
     * @return A list of all medication administrations for this patient (eg: medication administrations)
     */
    public Iterable<Administration> getAdministrations() {
        return administrations;
    }

    public void setAdministrations(List<Administration> administrations) {
        this.administrations = administrations;
    }

    /**
     *
     * @return FamilyHistory for this patient
     */
    public Iterable<FamilyHistory> getFamilyHistories() {
        return familyHistories;
    }

    public void setFamilyHistories(List<FamilyHistory> familyHistories) {
        this.familyHistories = familyHistories;
    }

    /**
     *
     * @return SocialHistory for this patient
     */
    public Iterable<SocialHistory> getSocialHistories() {
        return socialHistories;
    }

    public void setSocialHistories(List<SocialHistory> socialHistories) {
        this.socialHistories = socialHistories;
    }

    /**
     *
     * @return the primary demographics information associated with the patient
     */
    public Demographics getPrimaryDemographics() {
        return primaryDemographics;
    }

    public void setPrimaryDemographics(Demographics primaryDemographics) {
        this.primaryDemographics = primaryDemographics;
    }

    /**
     *
     * @return a list of alternate demographic information (Eg: aliases)
     */
    public Iterable<Demographics> getAlternateDemographics() {
        return alternateDemographics;
    }

    public void setAlternateDemographics(List<Demographics> supplementaryDemographics) {
        this.alternateDemographics = supplementaryDemographics;
    }

    /**
     *
     * @return the primary contact details associated with this patient
     */
    public ContactDetails getPrimaryContactDetails() {
        return primaryContactDetails;
    }

    public void setPrimaryContactDetails(ContactDetails primaryContactDetails) {
        this.primaryContactDetails = primaryContactDetails;
    }

    /**
     *
     * @return A list of bio metric values (eg: vitals) that have been measured for this patient
     */
    public Iterable<BiometricValue> getBiometricValues() {
        return biometricValues;
    }

    public void setBiometricValues(List<BiometricValue> biometricValues) {
        this.biometricValues = biometricValues;
    }

    /**
     *
     * @return A list of allergy events (allergen + reaction) for this patient
     */
    public Iterable<Allergy> getAllergies() {
        return allergies;
    }

    public void setAllergies(List<Allergy> allergies) {
        this.allergies = allergies;
    }

    /**
     *
     * @return ID of this patient in other external systems
     */
    public Iterable<ExternalID> getExternalIDs() {
        return externalIDs;
    }

    public void setExternalIDs(Set<ExternalID> externalIDs) {
        this.externalIDs = externalIDs;
    }

    public void setSources(List<Source> sources) {
        this.sources = sources;
        sourceMap.clear();
        populateSources();
    }

    /**
     *
     * @return The list of sources from which this patient object was constructed
     */
    public Iterable<Source> getSources() {
        return sources;
    }

    public void setParsingDetails(List<ParsingDetail> parsingDetails) {
        this.parsingDetails = parsingDetails;
        parsingDetailMap.clear();
        populateParsingDetails();
    }

    /**
     *
     * @return A list of all the parsers and associated metadata that was used in the construction of this patient object
     */
    public Iterable<ParsingDetail> getParsingDetails() {
        return parsingDetails;
    }

    public void setClinicalActors(List<ClinicalActor> clinicalActors) {
        this.clinicalActors = clinicalActors;
        clinicalActorMap.clear();
        populateClinicalActors();
    }

    /**
     *
     * @return A list of all clinical actors associated with the information in this patient record
     */
    public Iterable<ClinicalActor> getClinicalActors() {
        return clinicalActors;
    }

    public void setCareSites(List<CareSite> careSites) {
        this.careSites = careSites;
        careSiteMap.clear();
        populateCareSites();
    }

    /**
     *
     * @return A list of all care sites that are associated with this patient record
     */
    public Iterable<CareSite> getCareSites() {
        return careSites;
    }

    public Iterable<Event> getClinicalEvents() {
        return clinicalEvents;
    }

    public void addClinicalEvent(Event e) {
        // add the event only if its for this patient, and not for any other
        // patient.
        if(e.getPatientUUID().equals(this.getPatientId())) {
            this.clinicalEvents.add(e);
        }
        else {
            e.setPatientUUID(this.getPatientId());
            this.clinicalEvents.add(e);
        }
    }


    /**
     *
     * @return Coverage for this patient
     */
    public Iterable<Coverage> getCoverage() {
        return coverage;
    }

    public void setCoverage(List<Coverage> coverage) {
        this.coverage = coverage;
    }

    public void addCoverage(Coverage c) {
        this.coverage.add(c);
    }



    public void setClinicalEvents(Collection<Event> events) {
        this.clinicalEvents = events;
    }

    private void populateParsingDetails()
    {
        if (this.parsingDetails != null && !this.parsingDetails.isEmpty())
        {
            for (ParsingDetail p : this.parsingDetails)
            {
                this.parsingDetailMap.put(p.getParsingDetailsId(), p);
            }
        }
    }

    private void populateClinicalActors()
    {
        if (this.clinicalActors != null && !this.clinicalActors.isEmpty())
        {
            for (ClinicalActor c : this.clinicalActors)
            {
                this.clinicalActorMap.put(c.getInternalUUID(), c);
            }
        }
    }

    private void populateCareSites()
    {
        if (this.careSites != null && !this.careSites.isEmpty())
        {
            for (CareSite c : this.careSites)
            {
                this.careSiteMap.put(c.getInternalUUID(), c);
            }
        }
    }

    private void populateEncounters()
    {
        if (this.encounters != null && !this.encounters.isEmpty())
        {
            for (Encounter e : this.encounters)
            {
                this.encounterMap.put(e.getInternalUUID(), e);
            }
        }
    }

    private void populateSources()
    {
        if (this.sources != null && !this.sources.isEmpty())
        {
            for (Source s : this.sources)
            {
                this.sourceMap.put(s.getInternalUUID(), s);
                this.sourceMap.put(s.getSourceId(), s);
            }
        }
    }

    private Map<UUID, ParsingDetail> getParsingDetailMap()
    {
        return parsingDetailMap;
    }

    private Map<UUID, ClinicalActor> getClinicalActorMap()
    {
        return clinicalActorMap;
    }

    private Map<UUID, CareSite> getCareSiteMap()
    {
        return careSiteMap;
    }

    private Map<UUID, Encounter> getEncounterMap()
    {
        return encounterMap;
    }

    private Map<UUID, Source> getSourceMap()
    {
        return sourceMap;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, Constants.TO_STRING_STYLE).
           append("externalIDs", externalIDs).
           append("primaryDemographics", primaryDemographics).
           append("supplementaryDemographics", alternateDemographics).
           append("primaryContactDetails", primaryContactDetails).
           append("supplementaryContactDetails", alternateContactDetails).
           append("clinicalActors", clinicalActors).
           append("allergies", allergies).
           append("prescriptions", prescriptions).
           append("administrations", administrations).
           append("problems", problems).
           append("procedures", procedures).
           append("labs", labs).
           append("biometricValues", biometricValues).
           append("familyHistories", familyHistories).
           append("socialHistories", socialHistories).
           append("careSites", careSites).
           append("encounters", encounters).
           append("parsingDetail", parsingDetails).
           append("sources", sources).
           append("coverage", coverage).
           appendSuper(super.toString()).
           toString();
    }


    @Override
    public int hashCode() {
        HashCodeBuilder hashCodeBuilder = new HashCodeBuilder()
                .append(this.familyHistories).append(this.socialHistories)
                .append(this.primaryContactDetails)
                .append(this.primaryDemographics).appendSuper(super.hashCode());

        if(this.problems!=null){
            hashCodeBuilder.append(this.problems.toArray());
        }

        if(this.administrations!=null){
            hashCodeBuilder.append(this.administrations.toArray());
        }

        if(this.allergies!=null){
            hashCodeBuilder.append(this.allergies.toArray());
        }

        if(this.alternateContactDetails!=null){
            hashCodeBuilder.append(this.alternateContactDetails.toArray());
        }

        if(this.alternateDemographics!=null){
            hashCodeBuilder.append(this.alternateContactDetails.toArray());
        }

        if(this.biometricValues!=null){
            hashCodeBuilder.append(this.biometricValues.toArray());
        }

        if(this.careSites!=null){
            hashCodeBuilder.append(this.careSites.toArray());
        }

        if(this.clinicalActors!=null){
            hashCodeBuilder.append(this.clinicalActors.toArray());
        }

        if(this.documents!=null){
            hashCodeBuilder.append(this.documents.toArray());
        }

        if(this.encounters!=null){
            hashCodeBuilder.append(this.encounters.toArray());
        }

        if(this.externalIDs!=null){
            hashCodeBuilder.append(this.externalIDs.toArray());
        }

        if(this.labs!=null){
            hashCodeBuilder.append(this.labs.toArray());
        }

        if(this.prescriptions!=null){
            hashCodeBuilder.append(this.prescriptions.toArray());
        }

        if(this.procedures!=null){
            hashCodeBuilder.append(this.procedures.toArray());
        }

        if(this.sources!=null){
            hashCodeBuilder.append(this.sources.toArray());
        }

        if (this.coverage!= null){
            hashCodeBuilder.append(this.coverage.toArray());
        }

        return hashCodeBuilder.toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if(obj!=null && obj instanceof Patient){
            Patient p = (Patient) obj;
            EqualsBuilder equalsBuilder = new EqualsBuilder()
                    .append(this.familyHistories, p.familyHistories)
                    .append(this.socialHistories, p.socialHistories)
                    .append(this.primaryContactDetails, p.primaryContactDetails)
                    .append(this.primaryDemographics, p.primaryDemographics)
                    .appendSuper(super.equals(obj));

            if(this.problems!=null && p.problems!=null){
                Object[] arr1 = this.problems.toArray();
                Object[] arr2 = p.problems.toArray();
                Arrays.sort(arr1);
                Arrays.sort(arr2);
                equalsBuilder.append(arr1, arr2);
            }else if(this.problems!=null || p.problems!=null){
                return false;
            }

            if(this.administrations!=null && p.administrations!=null){
                Object[] arr1 = this.administrations.toArray();
                Object[] arr2 = p.administrations.toArray();
                Arrays.sort(arr1);
                Arrays.sort(arr2);
                equalsBuilder.append(arr1, arr2);
            }else if(this.administrations!=null || p.administrations!=null){
                return false;
            }

            if(this.allergies!=null && p.allergies!=null){
                Object[] arr1 = this.allergies.toArray();
                Object[] arr2 = p.allergies.toArray();
                Arrays.sort(arr1);
                Arrays.sort(arr2);
                equalsBuilder.append(arr1, arr2);
            }else if(this.allergies!=null || p.allergies!=null){
                return false;
            }

            if(this.alternateContactDetails!=null && p.alternateContactDetails!=null){
                Object[] arr1 = this.alternateContactDetails.toArray();
                Object[] arr2 = p.alternateContactDetails.toArray();
                Arrays.sort(arr1);
                Arrays.sort(arr2);
                equalsBuilder.append(arr1, arr2);
            }else if(this.alternateContactDetails!=null || p.alternateContactDetails!=null){
                return false;
            }

            if(this.alternateDemographics!=null && p.alternateDemographics!=null){
                Object[] arr1 = this.alternateDemographics.toArray();
                Object[] arr2 = p.alternateDemographics.toArray();
                Arrays.sort(arr1);
                Arrays.sort(arr2);
                equalsBuilder.append(arr1, arr2);
            }else if(this.alternateDemographics!=null || p.alternateDemographics!=null){
                return false;
            }

            if(this.biometricValues!=null && p.biometricValues!=null){
                Object[] arr1 = this.biometricValues.toArray();
                Object[] arr2 = p.biometricValues.toArray();
                Arrays.sort(arr1);
                Arrays.sort(arr2);
                equalsBuilder.append(arr1, arr2);
            }else if(this.biometricValues!=null || p.biometricValues!=null){
                return false;
            }

            if(this.careSites!=null && p.careSites!=null){
                Object[] arr1 = this.careSites.toArray();
                Object[] arr2 = p.careSites.toArray();
                Arrays.sort(arr1);
                Arrays.sort(arr2);
                equalsBuilder.append(arr1, arr2);
            }else if(this.biometricValues!=null || p.biometricValues!=null){
                return false;
            }

            if(this.clinicalActors!=null && p.clinicalActors!=null){
                Object[] arr1 = this.clinicalActors.toArray();
                Object[] arr2 = p.clinicalActors.toArray();
                Arrays.sort(arr1);
                Arrays.sort(arr2);
                equalsBuilder.append(arr1, arr2);
            }else if(this.clinicalActors!=null || p.clinicalActors!=null){
                return false;
            }

            if(this.documents!=null && p.documents!=null){
                Object[] arr1 = this.documents.toArray();
                Object[] arr2 = p.documents.toArray();
                Arrays.sort(arr1);
                Arrays.sort(arr2);
                equalsBuilder.append(arr1, arr2);
            }else if(this.documents!=null || p.documents!=null){
                return false;
            }

            if(this.encounters!=null && p.encounters!=null){
                Object[] arr1 = this.encounters.toArray();
                Object[] arr2 = p.encounters.toArray();
                Arrays.sort(arr1);
                Arrays.sort(arr2);
                equalsBuilder.append(arr1, arr2);
            }else if(this.encounters!=null || p.encounters!=null){
                return false;
            }

            if(this.externalIDs!=null && p.externalIDs!=null){
                Object[] arr1 = this.externalIDs.toArray();
                Object[] arr2 = p.externalIDs.toArray();
                Arrays.sort(arr1);
                Arrays.sort(arr2);
                equalsBuilder.append(arr1, arr2);
            }else if(this.externalIDs!=null || p.externalIDs!=null){
                return false;
            }

            if(this.labs!=null && p.labs!=null){
                Object[] arr1 = this.labs.toArray();
                Object[] arr2 = p.labs.toArray();
                Arrays.sort(arr1);
                Arrays.sort(arr2);
                equalsBuilder.append(arr1, arr2);
            }else if(this.labs!=null || p.labs!=null){
                return false;
            }

            if(this.labs!=null && p.labs!=null){
                Object[] arr1 = this.labs.toArray();
                Object[] arr2 = p.labs.toArray();
                Arrays.sort(arr1);
                Arrays.sort(arr2);
                equalsBuilder.append(arr1, arr2);
            }else if(this.labs!=null || p.labs!=null){
                return false;
            }



            if(this.procedures!=null && p.procedures!=null){
                Object[] arr1 = this.procedures.toArray();
                Object[] arr2 = p.procedures.toArray();
                Arrays.sort(arr1);
                Arrays.sort(arr2);
                equalsBuilder.append(arr1, arr2);
            }else if(this.procedures!=null || p.procedures!=null){
                return false;
            }

            if(this.sources!=null && p.sources!=null){
                Object[] arr1 = this.sources.toArray();
                Object[] arr2 = p.sources.toArray();
                Arrays.sort(arr1);
                Arrays.sort(arr2);
                equalsBuilder.append(arr1, arr2);
            }else if(this.sources!=null || p.sources!=null){
                return false;
            }

            if(this.coverage!=null && p.coverage!=null){
                Object[] arr1 = this.coverage.toArray();
                Object[] arr2 = p.coverage.toArray();
                Arrays.sort(arr1);
                Arrays.sort(arr2);
                equalsBuilder.append(arr1, arr2);
            }else if(this.coverage!=null || p.coverage!=null){
                return false;
            }

            return equalsBuilder.isEquals();
        }
        return false;
    }
}
