package com.apixio.converters.base;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.text.ParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.apixio.model.external.CaretParser;
import com.apixio.model.external.CaretParserException;
import com.apixio.model.file.catalog.jaxb.generated.ApxCatalog;
import org.apache.commons.lang.time.DateUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import com.apixio.model.patient.ClinicalCode;
import com.apixio.model.patient.ExternalID;
import com.apixio.model.patient.Gender;
import com.apixio.utility.HashCalculator;

public class BaseUtils {

    private final static String [] datePatterns = new String [] {
        "yyyyMMdd",
        "yyyyMMddkkmmssZ",
        "yyyyMMddkkmmss",
        "yyyy-MM-dd'T'HH:mm:ssZ",
        "yyyy-MM-dd",
        "MM/dd/yyyy HH:mm:ss aaa",
        "MM/dd/yyyy",
        "yyyy-MM-dd hh:ss:mm.sss",
        "yyyy-MM-dd hh:ss:mm"
    };

    public static boolean isEmpty(Iterable i) {
        return !i.iterator().hasNext();
    }

    public static DateTime parseDateTime(String date) {
        if(date == null || date.trim().length() <= 0)
            return null;
        try {
            return new DateTime(DateUtils.parseDate(date, datePatterns));
        } catch (ParseException e) {
            // this happens when the date was not parsable by our default date time parser
            // so we need to try our other parser.
            try {
                DateTimeFormatter fmt = ISODateTimeFormat.dateTimeParser();
                return fmt.parseDateTime(date);
            } catch (IllegalArgumentException exc) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return null;
            }
        }
    }

    public static byte[] getBytesFromInputStream(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int nRead;
        byte[] data = new byte[16384];

        while ((nRead = is.read(data, 0, data.length)) != -1) {
          buffer.write(data, 0, nRead);
        }

        buffer.flush();

        return buffer.toByteArray();
    }

    public static String getMimeTypeFromFileName(String fileName) {
        String mimeType = "";
        if (fileName != null) {
            int lastDot = fileName.lastIndexOf(".");
            if (lastDot > 0) {
                String extension = fileName.substring(lastDot);
                mimeType = getMimeTypeFromExtension(extension);
            }
        }
        return mimeType;
    }

    //TODO complete this method!!
    private static String getMimeTypeFromExtension(String extension) {
        String mimeType = "";
        if (extension.equalsIgnoreCase("jpg"))
            mimeType = "image/jpeg";
        return mimeType;
    }

    public static Set<ExternalID> getExternalIdsFromCatalog(List<ApxCatalog.CatalogEntry.Patient.PatientId> patientIds) {
        Set<ExternalID> externalIds = new HashSet<ExternalID>();
        if (patientIds != null) {
            for (ApxCatalog.CatalogEntry.Patient.PatientId patientId : patientIds) {
                externalIds.add(getExternalIdFromCatalog(patientId));
            }
        }
        return externalIds;
    }

    public static ExternalID getExternalIdFromCatalog(String externalId) throws CaretParserException {
        return CaretParser.toExternalID(externalId);
    }

    public static ExternalID getExternalIdFromCatalog(ApxCatalog.CatalogEntry.Patient.PatientId patientId) {
        ExternalID externalId = new ExternalID();
        externalId.setAssignAuthority(patientId.getAssignAuthority());
        externalId.setId(patientId.getId());
        return externalId;
    }


    public static ExternalID getExternalID(String id) {
        ExternalID externalID = new ExternalID();
        externalID.setSource(id);
        externalID.setAssignAuthority(id);
        return externalID;
    }

    public static ExternalID getExternalID(String extension, String root) {
        ExternalID externalID = new ExternalID();
        externalID.setAssignAuthority(root);
        externalID.setId(extension);
        return externalID;
    }

    public static Gender getGenderFromText(String genderText) {
        Gender gender = Gender.UNKNOWN;
        if (genderText != null) {
            if (genderText.equalsIgnoreCase("M") || genderText.equalsIgnoreCase("MALE"))
                gender = Gender.MALE;
            else if (genderText.equalsIgnoreCase("F") || genderText.equalsIgnoreCase("FEMALE"))
                gender = Gender.FEMALE;
            else if (genderText.equalsIgnoreCase("O") || genderText.equalsIgnoreCase("OTHER"))
                gender = Gender.TRANSGENDER;
        }
        return gender;
    }

    public static ClinicalCode getClinicalCode(String clinicalCodeString) {
        ClinicalCode clinicalCode = new ClinicalCode();
        clinicalCode.setDisplayName(clinicalCodeString);
        return clinicalCode;
    }
}
