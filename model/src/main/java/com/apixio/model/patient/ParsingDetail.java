package com.apixio.model.patient;

import java.net.URI;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.joda.time.DateTime;

import com.apixio.model.Constants;
import com.apixio.model.WithMetadata;
import com.apixio.model.utility.ApixioDateDeserialzer;
import com.apixio.model.utility.ApixioDateSerializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public class ParsingDetail extends WithMetadata {

	private UUID parsingDetailsId = UUID.randomUUID();
	private DateTime parsingDateTime;	// an associated date time object.
	private DocumentType contentSourceDocumentType;
	@Deprecated
	private URI contentSourceDocumentURI;	// the uri for the source document. - the S3 uri from which this piece of data was parsed?
//	private String sourceFileHash;
//	private UUID sourceFileArchiveUUID;
	private ParserType parser;
	private String parserVersion;

    private static String sourceFileHashTag = "sourceFileHash";
    private static String sourceFileArchiveUUIDTag = "sourceFileArchiveUUID";
    private static String sourceUploadBatchTag = "sourceUploadBatch";
	
	public UUID getParsingDetailsId() {
		return parsingDetailsId;
	}
	public void setParsingDetailsId(UUID parsingDetailsId) {
		this.parsingDetailsId = parsingDetailsId;
	}
	
	@JsonSerialize(using=ApixioDateSerializer.class)
	public DateTime getParsingDateTime() {
		return parsingDateTime;
	}
	
	@JsonDeserialize(using=ApixioDateDeserialzer.class)
	public void setParsingDateTime(DateTime parsingDateTime) {
		this.parsingDateTime = parsingDateTime;
	}
	public DocumentType getContentSourceDocumentType() {
		return contentSourceDocumentType;
	}
	public void setContentSourceDocumentType(DocumentType contentSourceDocumentType) {
		this.contentSourceDocumentType = contentSourceDocumentType;
	}
    @Deprecated
	public URI getContentSourceDocumentURI() {
		return contentSourceDocumentURI;
	}
    @Deprecated
	public void setContentSourceDocumentURI(URI contentSourceDocumentURI) {
		this.contentSourceDocumentURI = contentSourceDocumentURI;
	}

    @JsonIgnore
    public String getSourceFileHash() {
        return this.getMetaTag(sourceFileHashTag);
    }
    @JsonIgnore
    public void setSourceFileHash(String fileHash) {
        this.setMetaTag(sourceFileHashTag, fileHash);
    }
    @JsonIgnore
    public UUID getSourceFileArchiveUUID() {
        String uuid = this.getMetaTag(sourceFileArchiveUUIDTag);
        return uuid == null ? null : UUID.fromString(uuid);
    }
    @JsonIgnore
    public void setSourceFileArchiveUUID(UUID archiveUUID) {
        this.setMetaTag(sourceFileArchiveUUIDTag, archiveUUID.toString());
    }
    @JsonIgnore
    public String getSourceUploadBatch()
    {
        return this.getMetaTag(sourceUploadBatchTag);
    }
    @JsonIgnore
    public void setSourceUploadBatch(String uploadBatchName)
    {
        this.setMetaTag(sourceUploadBatchTag, uploadBatchName);
    }

	public ParserType getParser() {
		return parser;
	}
	public void setParser(ParserType parser) {
		this.parser = parser;
	}
	public String getParserVersion() {
		return parserVersion;
	}
	public void setParserVersion(String parserVersion) {
		this.parserVersion = parserVersion;
	}
	@Override
	public String toString() {
		return new ToStringBuilder(this, Constants.TO_STRING_STYLE).
	       append("parsingDetailsId", parsingDetailsId).
	       append("parsingDateTime", parsingDateTime).
	       append("contentSourceDocumentType", contentSourceDocumentType).
	       append("contentSourceDocumentURI", contentSourceDocumentURI).
	       append("parser", parser).
	       append("parserVersion", parserVersion).
	       appendSuper(super.toString()).
	       toString();
	}
}
