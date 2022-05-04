package com.apixio.util.nassembly;

import com.apixio.messages.OcrMessages;
import com.apixio.model.EitherStringOrNumber;
import com.apixio.datacatalog.*;
import com.apixio.model.patient.ExternalID;
import com.apixio.model.patient.Gender;
import com.apixio.model.patient.MaritalStatus;
import com.apixio.model.patient.ResolutionStatus;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import java.math.BigDecimal;
import java.util.UUID;


//TODO break up into separate util classes
public class DataCatalogProtoUtils {

    public static DateTime toDateTime(YearMonthDayOuterClass.YearMonthDay date) {
        if (date == null || date.getSerializedSize() == 0) return null;
        return new DateTime(date.getEpochMs());
    }

    public static String dateToYYYYMMDD(YearMonthDayOuterClass.YearMonthDay date) {
        if (date == null || date.getSerializedSize() == 0) return "";
        String dayAsString = String.valueOf(date.getDay());
        String paddedDay = date.getDay() < 10 ? "0" + dayAsString : dayAsString;

        String monthAsString = String.valueOf(date.getMonth());
        String paddedMonth = date.getMonth() < 10 ? "0" + monthAsString : monthAsString;
        return String.format("%s-%s-%s", date.getYear(), paddedMonth, paddedDay);
    }

    public static YearMonthDayOuterClass.YearMonthDay fromDateTime(DateTime date) {
        YearMonthDayOuterClass.YearMonthDay.Builder builder = YearMonthDayOuterClass.YearMonthDay.newBuilder();
        builder.setDay(date.getDayOfMonth());
        builder.setMonth(date.getMonthOfYear());
        builder.setYear(date.getYear());
        builder.setEpochMs(date.getMillis());
        return builder.build();
    }

    public static YearMonthDayOuterClass.YearMonthDay fromLocalDate(LocalDate date) {
        DateTime dateTime = date.toDateTimeAtStartOfDay();
        return fromDateTime(dateTime);
    }

    public static UUID convertUuid(UUIDOuterClass.UUID uuid) {
        if (uuid == null || uuid.getSerializedSize() == 0) return null;
        return UUID.fromString(uuid.getUuid());
    }

    public static UUIDOuterClass.UUID convertUuid(UUID uuid) {
        if (uuid == null) return null;
        return UUIDOuterClass.UUID.newBuilder()
                .setUuid(uuid.toString())
                .build();
    }

    public static EitherStringOrNumber convertValueAsString(ValueAsStringOuterClass.ValueAsString value) {
        if (value == null|| value.getSerializedSize() == 0) return null;

        if (value.getType() == ValueAsStringOuterClass.Type.BigDecimal) {
            return new EitherStringOrNumber(new BigDecimal(value.getValue()));
        }

        if (value.getType() == ValueAsStringOuterClass.Type.Long) {
            return new EitherStringOrNumber(new Long(value.getValue()));
        }

        if (value.getType() == ValueAsStringOuterClass.Type.Double) {
            return new EitherStringOrNumber(new Double(value.getValue()));
        }

        if (value.getType() == ValueAsStringOuterClass.Type.Float) {
            return new EitherStringOrNumber(new Float(value.getValue()));
        }

        if (value.getType() == ValueAsStringOuterClass.Type.Int) {
            return new EitherStringOrNumber(new Integer(value.getValue()));
        }

        // default to string
        return new EitherStringOrNumber(value.getValue());
    }

    public static Gender convertGender(GenderOuterClass.Gender gender) {
        switch(gender) {
            case MALE: return Gender.MALE;
            case FEMALE: return Gender.FEMALE;
            default: return Gender.UNKNOWN;
        }
    }

    public static MaritalStatus convertMaritalStatus(MaritalStatusOuterClass.MaritalStatus status) {
        try {
            return MaritalStatus.valueOf(status.name());
        } catch (IllegalArgumentException iae) {
            return MaritalStatus.NEVER_MARRIED;
        }
    }

    public static ResolutionStatus convertResolutionStatus(ResolutionStatusOuterClass.ResolutionStatus status) {
        switch (status) {
            case STILL_ACTIVE:
                return ResolutionStatus.ACTIVE;
            case INACTIVE:
                return ResolutionStatus.INACTIVE;
            case RESOLVED_:
                return ResolutionStatus.RESOLVED;
            case RECURRENCE:
                return ResolutionStatus.RECURRENCE;
            case RELAPSE:
                return ResolutionStatus.RELAPSE;
            case REMISSION:
                return ResolutionStatus.REMISSION;
            case UNKNOWN_STATUS:
                return null;
            default:
                return null;
        }
    }

    public static PatientProto.CodedBasePatient convertCodedBaseSummary(SummaryObjects.CodedBaseSummary summary) {
        PatientProto.CodedBasePatient.Builder builder = PatientProto.CodedBasePatient.newBuilder();

        builder.setDataCatalogMeta(summary.getDataCatalogMeta());
        builder.setPatientMeta(summary.getPatientMeta());
        builder.addAllSources(summary.getSourcesList());
        builder.addAllParsingDetails(summary.getParsingDetailsList());
        builder.addAllEncounters(summary.getEncountersList());
        if (summary.hasPrimaryActor())
            builder.addClinicalActors(summary.getPrimaryActor());
        builder.addAllClinicalActors(summary.getSupplementaryActorsList());

        return builder.build();
    }

    public static PatientProto.BasePatient convertBaseSummary(SummaryObjects.BaseSummary summary) {
        PatientProto.BasePatient.Builder builder = PatientProto.BasePatient.newBuilder();

        builder.setDataCatalogMeta(summary.getDataCatalogMeta());
        builder.setPatientMeta(summary.getPatientMeta());
        builder.addAllSources(summary.getSourcesList());
        builder.addAllParsingDetails(summary.getParsingDetailsList());

        return builder.build();
    }

    public static ExternalID convertOriginalIdToExternalId(ExternalIdOuterClass.ExternalId protoExternalId) {
        ExternalID externalID = new ExternalID();
        externalID.setId(protoExternalId.getId());
        externalID.setAssignAuthority(protoExternalId.getAssignAuthority());
        return externalID;
    }

    public static DocumentMetaOuterClass.DocumentMeta extractDocumentMeta(OcrMessages.OcrResponse ocrResponse) {
        if (ocrResponse.getSerializedSize() == 0)
            return null;

        DocumentMetaOuterClass.DocumentMeta.Builder builder = DocumentMetaOuterClass.DocumentMeta.newBuilder();
        builder.setUuid(UUIDOuterClass.UUID.newBuilder().setUuid(ocrResponse.getDocumentID().getUuid()).build());

        DocumentMetaOuterClass.OcrMetadata.Builder ocrMetaBuilder = DocumentMetaOuterClass.OcrMetadata.newBuilder();

        if (ocrResponse.hasResult()){
            OcrMessages.Result result = ocrResponse.getResult();

            if (result.hasOcrMeta()) {
                OcrMessages.OcrMeta ocrResult = result.getOcrMeta();
                ocrMetaBuilder.setStatus(ocrResult.getStatus());

                ocrMetaBuilder.setStatus(ocrResult.getStatus());
                ocrMetaBuilder.setResolution(ocrResult.getResolution());
                ocrMetaBuilder.setTimestampMs(ocrResult.getTimestampMs());
                ocrMetaBuilder.setTotalPages(DefaultValueWrapperUtils.wrapInt(ocrResult.getTotalPages()));
                ocrMetaBuilder.setAvailablePages(DefaultValueWrapperUtils.wrapInt(ocrResult.getAvailablePages()));


                builder.setOcrMetadata(ocrMetaBuilder.build());

                // If fully succeeded, set up content metadata [We don't write String Content if Partial Success]
                if (result.getStatus().equals(OcrMessages.OcrStatus.FULLY_SUCCEEDED)) {
                    long now = DateTime.now().getMillis();
                    DocumentMetaOuterClass.ContentMetadata.Builder contentBuilder = DocumentMetaOuterClass.ContentMetadata.newBuilder();
                    contentBuilder.setDocCacheLevel("doc");

                    contentBuilder.setStringContentMs(DefaultValueWrapperUtils.wrapLong(now));
                    contentBuilder.setDocCacheTsMs(DefaultValueWrapperUtils.wrapLong(now));
                    contentBuilder.setDocCacheFormat("protobuf_base64");
                    builder.setContentMeta(contentBuilder.build());
                }
            }
        }

        builder.setOcrMetadata(ocrMetaBuilder.build());
        return builder.build();
    }
}

