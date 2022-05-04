package com.apixio.model;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.builder.ToStringBuilder;


/**
 * WithMetadata is a useful baseclass, that can be added to classes to
 * automatically get metadata like features.
 * 
 * @author vishnu
 * 
 * @Uttama - We do not want to override euqals and hashcode method as it will impact the patient deduplication functionality.
 */
public abstract class WithMetadata {
	private Map<String, String> metadata = new HashMap<String, String>();

	public void setMetaTag(String tag, String value) {
		metadata.put(tag, value);
	}

	public String getMetaTag(String tag) {
		if (metadata.containsKey(tag))
			return metadata.get(tag);
		else
			return null;
	}

	public Map<String, String> getMetadata() {
		return metadata;
	}

	public void setMetadata(Map<String, String> metadata) {
		this.metadata = metadata;
	}
	@Override
	public String toString() {
		return new ToStringBuilder(this, Constants.TO_STRING_STYLE).
	       append("metadata", metadata).
	       toString();	
	}
}
