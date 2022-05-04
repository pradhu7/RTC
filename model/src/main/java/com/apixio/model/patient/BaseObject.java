package com.apixio.model.patient;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.joda.time.DateTime;

import com.apixio.model.Constants;
import com.apixio.model.IDGenerator;
import com.apixio.model.WithMetadata;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class BaseObject extends WithMetadata {

	private UUID sourceId; // A link to a source
	private UUID parsingDetailsId;
	private UUID internalUUID = UUID.randomUUID();
	private ExternalID originalId;
	private List<ExternalID> otherOriginalIds = new LinkedList<ExternalID>();
	
	private EditType editType = EditType.ACTIVE;
	private DateTime lastEditDateTime = DateTime.now();


	/**
	 * A function to generate a probabilistically unique internal id with
	 * minimal option of clashing within a patient record.
	 * 
	 * @return returns a long, which can be used as an internal id.
	 */
	public static long generateInternalID() {
		return IDGenerator.getInstance().nextId();
	}

	public UUID getSourceId() {
		return sourceId;
	}

	public void setSourceId(UUID sourceId) {
		this.sourceId = sourceId;
	}

	public UUID getInternalUUID() {
		return internalUUID;
	}

	public void setInternalUUID(UUID internalUUID) {
		this.internalUUID = internalUUID;
	}

	public void setParsingDetailsId(UUID parsingDetailsId) {
		this.parsingDetailsId = parsingDetailsId;
	}

	public UUID getParsingDetailsId() {
		return parsingDetailsId;
	}

	public void setOriginalId(ExternalID originalId) {
		this.originalId = originalId;
	}

	public ExternalID getOriginalId() {
		return originalId;
	}
	public void setOtherOriginalIds(List<ExternalID> otherOriginalIds) {
		this.otherOriginalIds = otherOriginalIds;
	}

	public List<ExternalID> getOtherOriginalIds() {
		return otherOriginalIds;
	}
	
	@Override
	public String toString() {
		return new ToStringBuilder(this, Constants.TO_STRING_STYLE).
	       append("sourceId", sourceId).
	       append("parsingDetailsId", parsingDetailsId).
	       append("internalUUID", internalUUID).
	       append("originalId", originalId).
	       append("OtherOriginalIds", otherOriginalIds).
	       appendSuper(super.toString()).
	       toString();	
	}

	@Override
	public boolean equals(Object obj) {
		if(obj!=null && obj instanceof BaseObject){
			BaseObject b = (BaseObject)obj;
			return new EqualsBuilder().append(this.sourceId, b.sourceId).append(this.originalId, b.originalId).isEquals();
		}
		return false;
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(sourceId).append(originalId).toHashCode();
	}

	public EditType getEditType() {
		return editType;
	}

	public void setEditType(EditType editType) {
		this.editType = editType;
		this.lastEditDateTime = DateTime.now();	// update lastEditDateTime whenever editType is updated
	}

	public DateTime getLastEditDateTime() {
		return lastEditDateTime;
	}

	public void setLastEditDateTime(DateTime lastEditDateTime) {
		this.lastEditDateTime = lastEditDateTime;
	}
	
	
}
