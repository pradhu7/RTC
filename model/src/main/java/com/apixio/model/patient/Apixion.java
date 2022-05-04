package com.apixio.model.patient;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang.builder.HashCodeBuilder;

import com.apixio.model.Builder;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * An apixion is the atomic concept unit that apixio recognizes. An apixion
 * usually has an apixionId (which uniquely identifies the apixion), but can
 * have other parameters such as lexical source, code, coding system, etc..
 * 
 * @author vishnu
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class Apixion extends BaseObject {
	private String apixionId;
	private String lexeme;
	private int startPos, endPos;
	private Map<String,Boolean> tagFlags = new HashMap<String, Boolean>();
	private Map<String,String> tags = new HashMap<String, String>();
	
	public static Builder<Apixion> newBuilder() 
	{
		return new ApixionBuilder();
	}
	
	// get a boolean equals and a hashcode method
	@Override
	public boolean equals(Object obj) 
	{
		Apixion other = (Apixion)(obj);
		return ((apixionId == other.apixionId) ||
				(apixionId != null &&
				(apixionId.equals(other.apixionId))));
	}
	
	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(apixionId).hashCode();
	}
	
	// builder
	@SuppressWarnings("unused")
	private static class ApixionBuilder implements Builder<Apixion> 
	{
		Apixion apx = new Apixion();
		
		public Apixion build() { return apx; }
		public ApixionBuilder setApixionId(String apxId) { apx.apixionId = apxId; return this; }
		public ApixionBuilder setLexeme(String l) {apx.lexeme = l; return this; }
		public ApixionBuilder setSourceId(UUID srcId) {apx.setSourceId(srcId); return this; }
		public ApixionBuilder setStartPos(int startPos) {apx.startPos = startPos; return this; }
		public ApixionBuilder setEndPos(int endPos) {apx.endPos = endPos; return this; }
		public ApixionBuilder setTagFlag(String key, boolean flag) {apx.setFlaggedWith(key,flag); return this; }
		public ApixionBuilder addTag(String tagName, String tagValue) {apx.setTagValue(tagName, tagValue); return this; }
	}
	
	// fancy accessor methods
	public boolean isFlaggedWith(String tag) 
	{
		return (tagFlags.containsKey(tag) && tagFlags.get(tag));
	}
	
	public String getTagValue(String tag) 
	{
		if (tags.containsKey(tag))
			return tags.get(tag);
		return null;
	}
	
	public void setFlaggedWith(String tag, boolean value) 
	{
		tagFlags.put(tag, value);
	}
	
	public void setTagValue(String tag, String tval) 
	{
		tags.put(tag, tval);
	}
	
	public String getApixionId() 
	{
		return apixionId;
	}
	
	public void setApixionId(String apixionId) 
	{
		this.apixionId = apixionId;
	}
	
	public String getLexeme() 
	{
		return lexeme;
	}
	public void setLexeme(String lexeme) 
	{
		this.lexeme = lexeme;
	}

	public int getStartPos() 
	{
		return startPos;
	}
	public void setStartPos(int startPos) 
	{
		this.startPos = startPos;
	}
	
	public int getEndPos() 
	{
		return endPos;
	}
	
	public void setEndPos(int endPos) 
	{
		this.endPos = endPos;
	}
	
	public Map<String, Boolean> getTagFlags() 
	{
		return tagFlags;
	}
	
	public void setTagFlags(Map<String, Boolean> tagFlags) 
	{
		this.tagFlags = tagFlags;
	}
	
	public Map<String, String> getTags() 
	{
		return tags;
	}
	
	public void setTags(Map<String, String> tags) 
	{
		this.tags = tags;
	}
}
