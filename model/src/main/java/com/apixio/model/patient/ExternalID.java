package com.apixio.model.patient;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;

import com.apixio.model.Constants;
import com.apixio.model.WithMetadata;

public class ExternalID extends WithMetadata {
	@Deprecated
	private String source;
	private String id;
	private String type;
	private String assignAuthority;
	
	public String getAssignAuthority() {
		return assignAuthority;
	}
	public void setAssignAuthority(String assignAuthority) {
		this.assignAuthority = assignAuthority;
	}
	@Deprecated
	public String getSource() {
		return source;
	}
	@Deprecated
	public void setSource(String source) {
		this.source = source;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	
	@Override
	public String toString() {
		return new ToStringBuilder(this, Constants.TO_STRING_STYLE).
	       append("id", id).
	       append("source", source).
	       append("assignAuthority", assignAuthority).
	       append("type", type).
	       appendSuper(super.toString()).
	       toString();
	}
	
	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(this.assignAuthority)
				.append(this.id).append(this.source).append(this.type)
				.toHashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj!=null && obj instanceof ExternalID){
			ExternalID id = (ExternalID)obj;
			return new EqualsBuilder()
					.append(this.assignAuthority, id.assignAuthority)
					.append(this.id, id.id).append(this.source, id.source)
					.append(this.type, id.type)
					.isEquals();
		}
		return false;
	}
	
	
}
