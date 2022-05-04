package com.apixio.model.patient;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.joda.time.DateTime;

import com.apixio.model.Constants;
import com.apixio.model.EitherStringOrNumber;
import com.apixio.model.utility.ApixioDateDeserialzer;
import com.apixio.model.utility.EitherStringOrNumberDeserializer;
import com.apixio.model.utility.EitherStringOrNumberSerializer;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonIgnoreProperties(ignoreUnknown=true)
public class LabResult extends CodedBaseObject {
	
	private String labName;
	private EitherStringOrNumber value;
	private String range;
	private LabFlags flag;
	private String units;
	private String labNote;
	private ClinicalCode specimen;
	private ClinicalCode panel;
	private ClinicalCode superPanel;
	private DateTime sampleDate;
	private UUID careSiteId;
	private List<TypedDate> otherDates = new LinkedList<TypedDate>();
	private Integer sequenceNumber;
			
	public String getLabName() {
		return labName;
	}
	public void setLabName(String labName) {
		this.labName = labName;
	}
	
	public DateTime getSampleDate() {
		return sampleDate;
	}
	
	@JsonDeserialize(as=DateTime.class,using=ApixioDateDeserialzer.class)
	public void setSampleDate(DateTime sampleDate) {
		this.sampleDate = sampleDate;
	}
	
	// the value of an object, is simply the non-null value, either its the right element or
	// its the left element.
	@JsonSerialize(using=EitherStringOrNumberSerializer.class)
	public EitherStringOrNumber getValue() {
	    return value;
	}
	
	@JsonDeserialize(using=EitherStringOrNumberDeserializer.class)
	public void setValue(EitherStringOrNumber value) {
		this.value = value;
	}
	
	public String getRange() {
		return range;
	}
	public void setRange(String range) {
		this.range = range;
	}
	public String getUnits() {
		return units;
	}
	public void setUnits(String units) {
		this.units = units;
	}

	public String getLabNote() {
		return labNote;
	}

	public void setLabNote(String labNote) {
		this.labNote = labNote;
	}

	public ClinicalCode getPanel() {
		return panel;
	}

	public void setPanel(ClinicalCode panel) {
		this.panel = panel;
	}

	public ClinicalCode getSuperPanel() {
		return superPanel;
	}

	public void setSuperPanel(ClinicalCode superPanel) {
		this.superPanel = superPanel;
	}

	public LabFlags getFlag() {
		return flag;
	}

	public void setFlag(LabFlags flag) {
		this.flag = flag;
	}

	public void setCareSiteId(UUID careSiteId) {
		this.careSiteId = careSiteId;
	}

	public UUID getCareSiteId() {
		return careSiteId;
	}

	public void setSpecimen(ClinicalCode specimen) {
		this.specimen = specimen;
	}

	public ClinicalCode getSpecimen() {
		return specimen;
	}
	public void setSequenceNumber(Integer sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}
	public Integer getSequenceNumber() {
		return sequenceNumber;
	}
	public void setOtherDates(List<TypedDate> otherDates) {
		this.otherDates = otherDates;
	}
	public List<TypedDate> getOtherDates() {
		return otherDates;
	}
	
	@Override
	public String toString() {
		return new ToStringBuilder(this, Constants.TO_STRING_STYLE).
	       append("labName", labName).
	       append("value", value).
	       append("range", range).
	       append("flag", flag).
	       append("units", units).
	       append("labNote", labNote).
	       append("specimen", specimen).
	       append("panel", panel).
	       append("superPanel", superPanel).
	       append("sampleDate", sampleDate).
	       append("careSiteId", careSiteId).
	       append("sequenceNumber", sequenceNumber).
	       append("otherDates", otherDates).
	       appendSuper(super.toString()).
	       toString();
	}
	
	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(this.careSiteId).append(this.flag)
				.append(this.labName).append(this.labNote).append(this.panel)
				.append(this.range).append(this.sampleDate)
				.append(this.specimen).append(this.superPanel)
				.append(this.units).append(this.value)
				.appendSuper(super.hashCode()).toHashCode();
	}	
	
	@Override
	public boolean equals(Object obj) {
		if(obj!=null && obj instanceof LabResult){
			LabResult lr = (LabResult)obj;
			return new EqualsBuilder().append(this.careSiteId, lr.careSiteId)
					.append(this.flag, lr.flag)
					.append(this.labName, lr.labName)
					.append(this.labNote, lr.labNote)
					.append(this.panel, lr.panel).append(this.range, lr.range)
					.append(this.sampleDate, lr.sampleDate)
					.append(this.specimen, lr.specimen)
					.append(this.superPanel, lr.superPanel)
					.append(this.units, lr.units).append(this.value, lr.value)
					.appendSuper(super.equals(obj)).isEquals();
		}
		return false;
	}
}
