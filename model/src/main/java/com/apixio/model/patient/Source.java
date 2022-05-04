package com.apixio.model.patient;

import java.util.UUID;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import com.apixio.model.Constants;

public class Source extends BaseObject {

	private String sourceSystem;	// the name of the source system we extracted this data from.
	private String sourceType;	// the type of source we extracted this data from.
	private UUID clinicalActorId;	// the clinical actor that was responsible for creating this source (could be a claims system)
	private Organization organization;	// the organization from which this source came about
	private DateTime creationDate;	// the creation date of the source, if we parse the data from say a CCR, then the creation date of the CCR goes here

    private LocalDate dciStart;

    private LocalDate dciEnd;
	
	public Source() {
		this.setSourceId(this.getInternalUUID());
	}
	
	public UUID getClinicalActorId() {
		return clinicalActorId;
	}
	public void setClinicalActorId(UUID clinicalActorId) {
		this.clinicalActorId = clinicalActorId;
	}
	public Organization getOrganization() {
		return organization;
	}
	public void setOrganization(Organization organization) {
		this.organization = organization;
	}
	public DateTime getCreationDate() {
		return creationDate;
	}
	public void setCreationDate(DateTime creationDate) {
		this.creationDate = creationDate;
	}
	public String getSourceSystem() {
		return sourceSystem;
	}
	public void setSourceSystem(String sourceSystem) {
		this.sourceSystem = sourceSystem;
	}
	public String getSourceType() {
		return sourceType;
	}
	public void setSourceType(String sourceType) {
		this.sourceType = sourceType;
	}

    public LocalDate getDciStart() {
        return dciStart;
    }

    public void setDciStart(LocalDate dciStart) {
        this.dciStart = dciStart;
    }

    public LocalDate getDciEnd() {
        return dciEnd;
    }

    public void setDciEnd(LocalDate dciEnd) {
        this.dciEnd = dciEnd;
    }
	
	@Override
	public String toString() {
		return new ToStringBuilder(this, Constants.TO_STRING_STYLE).
	       append("sourceSystem", sourceSystem).
	       append("sourceType", sourceType).
	       append("clinicalActorId", clinicalActorId).
	       append("organization", organization).
	       append("creationDate", creationDate).
           append("dciStart", dciStart).
           append("dciEnd", dciEnd).
	       appendSuper(super.toString()).
	       toString();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(this.creationDate)
				.append(this.sourceSystem).append(this.sourceType)
				.append(this.organization).append(this.clinicalActorId)
                .append(this.dciStart).append(this.dciEnd).appendSuper(super.hashCode())
				.toHashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if(obj!=null && obj instanceof Source){
			Source s = (Source) obj;
			return new EqualsBuilder()
					.append(this.creationDate, s.creationDate)
					.append(this.sourceSystem, s.sourceSystem)
					.append(this.sourceType, s.sourceType)
					.append(this.organization, s.organization)
					.append(this.clinicalActorId, s.clinicalActorId)
                    .append(this.dciStart, s.dciStart)
                    .append(this.dciEnd, s.dciEnd)
					.appendSuper(super.equals(obj)).isEquals();
		}
		return false;
	}
	
	
}
