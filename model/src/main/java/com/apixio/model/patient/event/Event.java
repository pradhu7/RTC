package com.apixio.model.patient.event;

import java.util.UUID;

import org.joda.time.DateTime;

import com.apixio.model.patient.CodedBaseObject;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


/**
 * An event is a lightweight abstraction around the elements inside a patient object. (which 
 * could be extracted from either the structured or unstructured data).
 *  
 * @author vvyas
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Event extends CodedBaseObject {
	private UUID patientUUID;
	private UUID objectUUID;
	private UUID parentEncounterID;
	private DateTime startDate, endDate;
	private String objectType;
	private String displayText;
	private DateTime validAfter = DateTime.now(), validBefore=null;
	private EventType eventType = EventType.CLINICAL;	// defaults to clinical event
		
	// accessor methods
	public UUID getPatientUUID() {
		return patientUUID;
	}
	public void setPatientUUID(UUID patientUUID) {
		this.patientUUID = patientUUID;
	}
	public UUID getObjectUUID() {
		return objectUUID;
	}
	public void setObjectUUID(UUID objectUUID) {
		this.objectUUID = objectUUID;
	}
	
	public DateTime getStartDate() {
		return startDate;
	}
	public void setStartDate(DateTime startDate) {
		this.startDate = startDate;
	}
	public DateTime getEndDate() {
		return endDate;
	}
	public void setEndDate(DateTime endDate) {
		this.endDate = endDate;
	}
	public String getObjectType() {
		return objectType;
	}
	public void setObjectType(String objectType) {
		this.objectType = objectType;
	}
	public String getDisplayText() {
		return displayText;
	}
	public void setDisplayText(String displayText) {
		this.displayText = displayText;
	}
	public DateTime getValidAfter() {
		return validAfter;
	}
	public void setValidAfter(DateTime validAfter) {
		this.validAfter = validAfter;
	}
	public DateTime getValidBefore() {
		return validBefore;
	}
	public void setValidBefore(DateTime validBefore) {
		this.validBefore = validBefore;
	}
	public EventType getEventType() {
		return eventType;
	}
	public void setEventType(EventType eventType) {
		this.eventType = eventType;
	}
	public UUID getParentEncounterID() {
		return parentEncounterID;
	}
	public void setParentEncounterID(UUID parentEncounterID) {
		this.parentEncounterID = parentEncounterID;
	}
}