package com.apixio.model.patient;

import java.net.URI;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class DocumentContent extends BaseObject {

	private long length;
	private String hash;
	private String mimeType;
	private URI uri;
	private transient byte[] content;
	
	public long getLength() {
		return length;
	}
	public void setLength(long length) {
		this.length = length;
	}
	public String getMimeType() {
		return mimeType;
	}
	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}
	public URI getUri() {
		return uri;
	}
	public void setUri(URI uri) {
		this.uri = uri;
	}
	@JsonIgnore
	public byte[] getContent() {
		return content;
	}
	@JsonIgnore
	public void setContent(byte[] content) {
		this.content = content;
	}
	public void setHash(String hash) {
		this.hash = hash;
	}
	public String getHash() {
		return hash;
	}

}
