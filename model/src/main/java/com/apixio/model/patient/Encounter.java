package com.apixio.model.patient;

import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.joda.time.DateTime;

import com.apixio.model.Constants;
import com.apixio.model.utility.ApixioDateDeserialzer;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonIgnoreProperties(ignoreUnknown=true)
public class Encounter extends CodedBaseObject {
	private DateTime encounterStartDate;
	private DateTime encounterEndDate;
	private EncounterType encType;
	private CareSite siteOfService;
	private List<ClinicalCode> chiefComplaints = new LinkedList<ClinicalCode>();
	
	@Deprecated
	public DateTime getEncounterDate() {
		return encounterStartDate;
	}
	
	@JsonDeserialize(as=DateTime.class,using=ApixioDateDeserialzer.class)
	@Deprecated
	public void setEncounterDate(DateTime encounterDate) {
		this.encounterStartDate = encounterDate;
	}
	
	public DateTime getEncounterStartDate() {
		return encounterStartDate;
	}
	
	@JsonDeserialize(as=DateTime.class,using=ApixioDateDeserialzer.class)
	public void setEncounterStartDate(DateTime encounterStartDate) {
		this.encounterStartDate = encounterStartDate;
	}
	
	public UUID getEncounterId() {
		return getInternalUUID();
	}

	public void setEncounterId(UUID encounterId) {
		setInternalUUID(encounterId);
	}

	public EncounterType getEncType() {
		return encType;
	}

	public void setEncType(EncounterType encType) {
		this.encType = encType;
	}

	public CareSite getSiteOfService() {
		return siteOfService;
	}

	public void setSiteOfService(CareSite siteOfService) {
		this.siteOfService = siteOfService;
	}

	public void setChiefComplaints(List<ClinicalCode> chiefComplaints) {
		this.chiefComplaints = chiefComplaints;
	}

	public List<ClinicalCode> getChiefComplaints() {
		if (chiefComplaints != null) {
			chiefComplaints = chiefComplaints.stream().filter(Objects::nonNull).collect(Collectors.toList());
		} else {
			chiefComplaints = new ArrayList<>();
		}

		return chiefComplaints;
	}
	
	@Override
	public String toString() {
		return new ToStringBuilder(this, Constants.TO_STRING_STYLE).
	       append("encounterId", getEncounterId()).
	       append("encounterDate", encounterStartDate).
	       append("encType", encType).
	       append("siteOfSerivce", siteOfService).
	       append("chiefComplaints", chiefComplaints).
	       appendSuper(super.toString()).
	       toString();
	}

	public DateTime getEncounterEndDate() {
		return encounterEndDate;
	}

	public void setEncounterEndDate(DateTime encounterEndDate) {
		this.encounterEndDate = encounterEndDate;
	}
}
