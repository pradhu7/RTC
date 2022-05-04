package com.apixio.model.patient;

import java.util.LinkedList;
import java.util.List;

import org.joda.time.DateTime;

import com.apixio.model.Builder;
import com.apixio.model.utility.ApixioDateDeserialzer;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonIgnoreProperties(ignoreUnknown=true)
public class Document extends CodedBaseObject {
	private String documentTitle;
	private DateTime documentDate;
	private String stringContent; // HTML for overlay (long term, just search text)
	private List<DocumentContent> documentContents = new LinkedList<DocumentContent>(); // various export formats
	
	@Override
	public String toString() {
		return String.format("%s (%s)",documentTitle, documentDate.toString());
	}
	
	public static final String DEFAULT_ENCODING = "UTF-8";
	
	public static DocumentBuilder newBuilder() {
		return new DocumentBuilder();
	}
	
	public static class DocumentBuilder implements Builder<Document> {
		
		private Document d = new Document();
		
		public Document build() {
			return d;
		}
		
		public DocumentBuilder setDocumentTitle(String docTitle) {
			d.documentTitle = docTitle;
			return this;
		}
		
		public DocumentBuilder setDocumentDate(DateTime date) {
			d.documentDate = date;
			return this;
		}
	}
	
	public String getStringContent() {
		return stringContent;
	}
	
	public void setStringContent(String s) {
		this.stringContent = s;
	}
	
	public String getDocumentTitle() {
		return documentTitle;
	}
	public void setDocumentTitle(String documentTitle) {
		this.documentTitle = documentTitle;
	}
	public DateTime getDocumentDate() {
		return documentDate;
	}
	
	@JsonDeserialize(as=DateTime.class,using=ApixioDateDeserialzer.class)
	public void setDocumentDate(DateTime documentDate) {
		this.documentDate = documentDate;
	}

	public void setDocumentContents(List<DocumentContent> documentContents) {
		this.documentContents = documentContents;
	}

	public List<DocumentContent> getDocumentContents() {
		return documentContents;
	}
	
	
}
