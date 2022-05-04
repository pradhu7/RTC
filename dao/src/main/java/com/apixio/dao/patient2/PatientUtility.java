package com.apixio.dao.patient2;

import java.util.Iterator;
import java.util.UUID;


import com.apixio.model.patient.ExternalID;
import com.apixio.model.patient.Patient;
import com.apixio.model.patient.Document;
import com.apixio.model.patient.Source;
import com.apixio.model.patient.ParsingDetail;
import com.apixio.model.patient.ParserType;
import org.apache.commons.lang.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PatientUtility
{
    private static final Logger logger = LoggerFactory.getLogger(PatientUtility.class);

    public static String getPatientKey(Patient patient) throws Exception
    {
        if (patient != null && patient.getPrimaryExternalID() != null)
        {
            ExternalID primaryId = patient.getPrimaryExternalID();
            if (primaryId != null)
            {
                // this is PHI we shouldn't log it
                // logger.info("AssignAuthority: " + primaryId.getAssignAuthority() + " ;ID: " + primaryId.getId() + " ;Type: " + primaryId.getType());

                // StringBuffer buff = new StringBuffer("").append(primaryId.getAssignAuthority()).append(primaryId.getId()).append(primaryId.getType());

                // THIS IS MUST FOR BACKWARD COMPATIBILITY
                String type = null;

                StringBuffer buff = new StringBuffer("").append(primaryId.getAssignAuthority()).append(primaryId.getId()).append(type);
                return buff.toString();
            }
        }
        return null;
    }

    public static String getPatientKey(String assignAuthority, String id)
    {

        StringBuffer buff = new StringBuffer("").append(assignAuthority).append(id).append("null");
        return buff.toString();

    }

    public static String getPatientKey(String ofAssignAuthority, Patient patient) throws Exception
    {
        ExternalID primaryId;

        if (patient != null && patient.getExternalIDs() != null)
        {
            Iterator<ExternalID> externalIds = patient.getExternalIDs().iterator();
            while (externalIds.hasNext())
            {
                primaryId = externalIds.next();
                if (ofAssignAuthority == null || (primaryId.getAssignAuthority() != null && primaryId.getAssignAuthority().trim().equalsIgnoreCase(ofAssignAuthority.trim())))
                {
                    // StringBuffer buff = new StringBuffer("").append(primaryId.getAssignAuthority()).append(primaryId.getId()).append(primaryId.getType());

                    String type = null;

                    StringBuffer buff = new StringBuffer("").append(primaryId.getAssignAuthority()).append(primaryId.getId()).append(type);
                    return buff.toString();
                }
            }
        }

        return null;
    }

    /**
     * Returns the Cassandra row key for patient.
     */
    public static String getDocumentKey(Patient patient) throws Exception
    {
        if (patient != null)
        {
            Iterable<Document> documents = patient.getDocuments();
            Iterator<Document> it = documents.iterator();

            if (it.hasNext())
            {
                Document document = it.next();
                Source docSource = patient.getSourceById(document.getSourceId());

                StringBuffer strBuffer = new StringBuffer("");

                if (docSource != null && docSource.getSourceSystem() != null && !docSource.getSourceSystem().equalsIgnoreCase("null"))
                    strBuffer.append(docSource.getSourceSystem().toLowerCase());

                if (document.getOriginalId() != null)
                {
                    if (document.getOriginalId().getAssignAuthority() != null && !document.getOriginalId().getAssignAuthority().equalsIgnoreCase("null"))
                        strBuffer.append(document.getOriginalId().getAssignAuthority().toLowerCase());
                    else if (document.getOriginalId().getSource() != null && !document.getOriginalId().getSource().equalsIgnoreCase("null"))
                        strBuffer.append(document.getOriginalId().getSource().toLowerCase());

                    if (document.getOriginalId().getId() != null && !document.getOriginalId().getId().equalsIgnoreCase("null"))
                        strBuffer.append(document.getOriginalId().getId().toLowerCase());
                }

                if (strBuffer.length() != 0)
                {
                    return strBuffer.toString();
                }
            }
            else // APOs will not contain documents if they were created from AXM uploads.
            {
                Iterator<ParsingDetail> pdit = patient.getParsingDetails().iterator();
                while (pdit.hasNext())
                {
                    ParsingDetail pd = pdit.next();
                    return pd.getSourceFileHash();
                }
            }
        }

        return null;
    }

    public static UUID getSourceFileArchiveUUID(Patient patient)
    {
        return getSourceFileArchiveUUID(patient, null);
    }

    public static UUID getSourceFileArchiveUUID(Patient patient, String uploadBatchId)
    {
        UUID documentUuid = null;

        if (patient != null)
        {
            Iterator<ParsingDetail> pdit = patient.getParsingDetails().iterator();
            while (pdit.hasNext())
            {
                ParsingDetail pd = pdit.next();
                if (StringUtils.isEmpty(uploadBatchId))
                {
                    documentUuid = pd.getSourceFileArchiveUUID();
                }
                else if (uploadBatchId.equals(pd.getSourceUploadBatch()))
                {
                    documentUuid = pd.getSourceFileArchiveUUID();
                }

                if (documentUuid != null)
                    break;
            }

            // This should only happen for OLD documents
            if (documentUuid == null && StringUtils.isEmpty(uploadBatchId))
            {
                Iterable<Document> documents = patient.getDocuments();
                Iterator<Document> it = documents.iterator();

                if (it.hasNext())
                {
                    Document document = it.next();
                    documentUuid = document.getInternalUUID();
                }
            }
        }

        return documentUuid;
    }

    public static String getUploadBatchId(Patient patient)
    {
        if (patient != null)
        {
            Iterator<ParsingDetail> pdit = patient.getParsingDetails().iterator();
            while (pdit.hasNext())
            {
                ParsingDetail pd = pdit.next();
                if (pd.getSourceUploadBatch() != null && !pd.getSourceUploadBatch().isEmpty())
                    return pd.getSourceUploadBatch();
            }
        }
        return null;
    }

    public static String getSourceSystem(Patient patient)
    {
        if (patient != null)
        {
            Iterator<Source> sit = patient.getSources().iterator();
            if (sit.hasNext())
                return sit.next().getSourceSystem();
        }
        return null;
    }
}
