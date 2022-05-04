package com.apixio.nassembly.apo.converterutils;

import com.apixio.datacatalog.DocumentContentOuterClass;
import com.apixio.datacatalog.DocumentMetaOuterClass;
import com.apixio.model.patient.Document;
import com.apixio.model.patient.DocumentContent;
import com.apixio.util.nassembly.CaretParser;
import com.apixio.nassembly.documentmeta.DocMetadataKeys;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.apixio.nassembly.apo.BaseConverter.wrapMetadataUpdate;
import static com.apixio.util.nassembly.DataCatalogProtoUtils.convertUuid;
import static com.apixio.util.nassembly.DataCatalogProtoUtils.dateToYYYYMMDD;

public class DocumentConverterUtils {

    /**
     * Set the metadata for the Document
     *
     * @param document model.patient.Document
     * @param proto    protobuf Document Meta
     */
    public static void setMetadatas(Document document, DocumentMetaOuterClass.DocumentMeta proto) {
        Map<String, String> metadata = document.getMetadata();

        setDocumentTopLevelMetadata(metadata, proto);
        if (proto.hasEncounterMeta()) setEncounterMetadata(metadata, proto.getEncounterMeta());
        if (proto.hasOcrMetadata()) setDocumentOcrMetadata(metadata, proto.getOcrMetadata());
        if (proto.hasContentMeta()) setContentMetadata(metadata, proto.getContentMeta());
        if (proto.hasHealthPlanMeta()) setDocumentHealthPlanMetadata(metadata, proto.getHealthPlanMeta());

        if (!proto.getContentMeta().hasTextExtractMs()) {
            // We need backwards compatible NOT_IMPLEMENTED
            metadata.put(DocMetadataKeys.TEXT_EXTRACTED().toString(), "NOT_IMPLEMENTED"); // legacy
        }
    }

    public static void setDocumentTopLevelMetadata(Map<String, String> metadata, DocumentMetaOuterClass.DocumentMeta proto) {
        // implicit empty when false
        if (proto.getDateAssumed()) {
            wrapMetadataUpdate(metadata, DocMetadataKeys.DATE_ASSUMED().toString(), String.valueOf(proto.getDateAssumed()));
        }

        wrapMetadataUpdate(metadata, DocMetadataKeys.DOCUMENT_TYPE().toString(), proto.getDocumentType());

    }

    public static void setEncounterMetadata(Map<String, String> metadata, DocumentMetaOuterClass.EncounterMetadata proto) {
        wrapMetadataUpdate(metadata, DocMetadataKeys.DOCUMENT_FORMAT().toString(), proto.getDocumentFormat());
        wrapMetadataUpdate(metadata, DocMetadataKeys.DOCUMENT_SIGNED_STATUS().toString(), proto.getDocumentSignedStatus());
        wrapMetadataUpdate(metadata, DocMetadataKeys.DOCUMENT_AUTHOR().toString(), proto.getDocumentAuthor());
        wrapMetadataUpdate(metadata, DocMetadataKeys.DOCUMENT_UPDATE().toString(), dateToYYYYMMDD(proto.getDocumentUpdate()));
        wrapMetadataUpdate(metadata, DocMetadataKeys.ENCOUNTER_DATE().toString(), dateToYYYYMMDD(proto.getEncounterDate()));
        wrapMetadataUpdate(metadata, DocMetadataKeys.PROVIDER_TYPE().toString(), proto.getProviderType());
        wrapMetadataUpdate(metadata, DocMetadataKeys.PROVIDER_TITLE().toString(), proto.getProviderTitle());
        wrapMetadataUpdate(metadata, DocMetadataKeys.PROVIDER_ID().toString(), proto.getProviderId());
        wrapMetadataUpdate(metadata, DocMetadataKeys.DOCUMENT_SIGNED_DATE().toString(), dateToYYYYMMDD(proto.getDocumentSignedDate()));

        if (proto.getEncounterProviderType().getSerializedSize() != 0) {
            wrapMetadataUpdate(metadata, DocMetadataKeys.ENCOUNTER_PROVIDER_TYPE().toString(), CaretParser.toString(proto.getEncounterProviderType()));
        }

        wrapMetadataUpdate(metadata, DocMetadataKeys.ENCOUNTER_DATE_START().toString(), dateToYYYYMMDD(proto.getEncounterStartDate()));
        wrapMetadataUpdate(metadata, DocMetadataKeys.ENCOUNTER_DATE_END().toString(), dateToYYYYMMDD(proto.getEncounterEndDate()));

        wrapMetadataUpdate(metadata, DocMetadataKeys.LST_FILED_INST_DTTM().toString(), dateToYYYYMMDD(proto.getLastEditTime()));
        wrapMetadataUpdate(metadata, DocMetadataKeys.ENT_INST_LOCAL_DTTM().toString(), dateToYYYYMMDD(proto.getEncounterTime()));
        wrapMetadataUpdate(metadata, DocMetadataKeys.DOCUMENT_UPDATE_DATE().toString(), dateToYYYYMMDD(proto.getDocumentUpdateDate()));
        wrapMetadataUpdate(metadata, DocMetadataKeys.ENTRY_INSTANT_DTTM().toString(), dateToYYYYMMDD(proto.getEntryInstantDate()));
        wrapMetadataUpdate(metadata, DocMetadataKeys.UPD_AUTHOR_INS_DTTM().toString(), dateToYYYYMMDD(proto.getUpdatedAuthorDate()));
        wrapMetadataUpdate(metadata, DocMetadataKeys.UPD_AUT_LOCAL_DTTM().toString(), dateToYYYYMMDD(proto.getUpdatedAuthorLocalDate()));
        wrapMetadataUpdate(metadata, DocMetadataKeys.NOT_FILETM_LOC_DTTM().toString(), dateToYYYYMMDD(proto.getNoteFileLocalDate()));
        wrapMetadataUpdate(metadata, DocMetadataKeys.NOTE_FILE_TIME_DTTM().toString(), dateToYYYYMMDD(proto.getNoteFileDate()));
        wrapMetadataUpdate(metadata, DocMetadataKeys.ENCOUNTER_ID().toString(), proto.getEncounterId());
        wrapMetadataUpdate(metadata, DocMetadataKeys.ENCOUNTER_NAME().toString(), proto.getEncounterName());
        wrapMetadataUpdate(metadata, DocMetadataKeys.NOTE_TYPE().toString(), proto.getNoteType());
    }

    public static void setDocumentOcrMetadata(Map<String, String> metadata, DocumentMetaOuterClass.OcrMetadata proto) {
        if (proto.getSerializedSize() > 0) {
            wrapMetadataUpdate(metadata, DocMetadataKeys.OCR_STATUS().toString(), proto.getStatus());
            wrapMetadataUpdate(metadata, DocMetadataKeys.OCR_RESOLUTION().toString(), proto.getResolution());

            wrapMetadataUpdate(metadata, DocMetadataKeys.OCR_CHILD_EXIT_CODE().toString(), String.valueOf(proto.getChildExitCode()));

            wrapMetadataUpdate(metadata, DocMetadataKeys.OCR_CHILD_EXIT_CODE_STR().toString(), proto.getChildExitCodeStr());

            if (proto.getTimestampMs() != 0) {
                wrapMetadataUpdate(metadata, DocMetadataKeys.OCR_TIMSTAMP().toString(), String.valueOf(proto.getTimestampMs()));
            }

            wrapMetadataUpdate(metadata, DocMetadataKeys.GS_VERSION().toString(), proto.getGsVersion());
            if (proto.hasTotalPages()) {
                wrapMetadataUpdate(metadata, DocMetadataKeys.TOTAL_PAGES().toString(), String.valueOf(proto.getTotalPages().getValue()));
            }
            if (proto.hasAvailablePages()) {
                wrapMetadataUpdate(metadata, DocMetadataKeys.AVAILABLE_PAGES().toString(), String.valueOf(proto.getAvailablePages().getValue()));
            }
        }
    }

    public static void setContentMetadata(Map<String, String> metadata, DocumentMetaOuterClass.ContentMetadata proto) {
        // Content Extract

        // 0L is the default value for Longs. 0 means no valid value
        if (proto.hasStringContentMs()) {
            wrapMetadataUpdate(metadata, DocMetadataKeys.STRING_CONTENT_TS().toString(), String.valueOf(proto.getStringContentMs().getValue()));
        }

        if (proto.hasTextExtractMs()) {
            wrapMetadataUpdate(metadata, DocMetadataKeys.TEXT_EXTRACTED_TS().toString(), String.valueOf(proto.getTextExtractMs().getValue()));
        }

        if (proto.hasDocCacheTsMs()) {
            wrapMetadataUpdate(metadata, DocMetadataKeys.DOC_CACHE_TS().toString(), String.valueOf(proto.getDocCacheTsMs().getValue()));
        }

        wrapMetadataUpdate(metadata, DocMetadataKeys.PIPELINE_VERSION().toString(), proto.getPipelineVersion());

        wrapMetadataUpdate(metadata, DocMetadataKeys.DOC_CACHE_LEVEL().toString(), proto.getDocCacheLevel());
        wrapMetadataUpdate(metadata, DocMetadataKeys.LEVEL().toString(), proto.getDocCacheLevel()); // same as doc_cache_level
        wrapMetadataUpdate(metadata, DocMetadataKeys.DOC_CACHE_FORMAT().toString(), proto.getDocCacheFormat());
    }

    public static void setDocumentHealthPlanMetadata(Map<String, String> metadata, DocumentMetaOuterClass.HealthPlanMetadata proto) {
        wrapMetadataUpdate(metadata, DocMetadataKeys.ALTERNATE_CHART_ID().toString(), CaretParser.toString(proto.getAlternateChartId()));
        wrapMetadataUpdate(metadata, DocMetadataKeys.CHART_TYPE().toString(), proto.getChartType());
    }


    public static void setDocumentContents(Document document, DocumentMetaOuterClass.DocumentMeta documentMeta) throws URISyntaxException {
        List<DocumentContent> contents = new ArrayList<>();
        for (DocumentContentOuterClass.DocumentContent docContent : documentMeta.getDocumentContentsList()) {
            DocumentContent converted = convertDocumentContent(docContent);
            contents.add(converted);
        }
        document.setDocumentContents(contents);
    }

    public static DocumentContent convertDocumentContent(DocumentContentOuterClass.DocumentContent proto) throws URISyntaxException {
        DocumentContent content = new DocumentContent();
        content.setLength(proto.getLength());
        content.setHash(proto.getHash());
        content.setMimeType(proto.getMimeType());
        if (!proto.getUri().isEmpty()) content.setUri(new URI(proto.getUri()));
        content.setParsingDetailsId(convertUuid(proto.getParsingDetailsId()));
        content.setSourceId(convertUuid(proto.getSourceId()));
        return content;
    }
}
