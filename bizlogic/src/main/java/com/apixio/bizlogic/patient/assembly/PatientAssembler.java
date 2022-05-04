package com.apixio.bizlogic.patient.assembly;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.apixio.bizlogic.patient.assembly.merge.PatientSet;
import com.apixio.dao.patient2.PatientDataUtility;
import com.apixio.model.patient.BaseObject;
import com.apixio.model.patient.ParsingDetail;
import com.apixio.model.patient.Patient;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.helpers.LogLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class PatientAssembler implements PrerequisiteAssembler<Patient,Patient>
{
    private static Logger logger = LoggerFactory.getLogger(PatientAssembler.class);

    private static PatientDataUtility dataUtility = new PatientDataUtility();

    public static final String BATCHID = "$batchId";

    public static String [] EVENTTAGS = {
             "$patientUUID",
             "$documentId",
             "$documentUUID",
             "$workId",
             "$jobId",
             "$orgId",
             BATCHID };

    @Override
    public byte[] serialize(Patient patient, String scope) throws Exception
    {
        return dataUtility.makePatientBytesWithScope(patient, scope, true);
    }

    @Override
    public Patient deserialize(byte[] bytes)
    {
        try
        {
            return reconcileExtract(dataUtility.getPatientObj(bytes, true));
        }
        catch (Exception ex)
        {
            return null;
        }
    }

    private Patient reconcileExtract(Patient patient)
    {
        if(patient == null)
        {
            return patient;
        }

        //
        // Reconciliation needed to happen, because older data had information placed into the base objects
        //
        // This will essentially be a no-op moving forward - when a final merge down has taken place. We will
        // continue to leave this here until the next migration/recompute that removes this from historical data
        //
        long start = System.currentTimeMillis();
        reconcileExtractEventMetadataWithParsingDetails(patient, patient.getProblems());

        if(logger.isDebugEnabled())
        {
            logger.debug("reconcile problems took" + TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - start) + "sec");
        }

        long start2 = System.currentTimeMillis();
        reconcileExtractEventMetadataWithParsingDetails(patient, patient.getProcedures());

        if(logger.isDebugEnabled())
        {
            logger.debug("reconcile procedures took" + TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - start2) + "sec");
        }

        long start3 = System.currentTimeMillis();
        reconcileExtractEventMetadataWithParsingDetails(patient, patient.getEncounters());

        if(logger.isDebugEnabled())
        {
            logger.debug("reconcile encounters took" + TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - start3) + "sec");
            logger.debug("total time to reconcileExtractEventMetadataWithParsingDetails took" + TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - start) + "sec");
        }
        return patient;
    }

    static private void reconcileExtractEventMetadataWithParsingDetails(Patient patient, Iterable<? extends BaseObject> codedBaseObjects)
    {
        if (codedBaseObjects == null)
        {
            return;
        }

        ParsingDetail masterParsingDetail;

        if (patient.getParsingDetails().iterator().hasNext())
        {
            masterParsingDetail = patient.getParsingDetails().iterator().next();
        }
        else
        {
            masterParsingDetail = new ParsingDetail();
            patient.addParsingDetail(masterParsingDetail);
        }

        for (BaseObject baseObject: codedBaseObjects)
        {
            for (String tag: EVENTTAGS)
            {
                //
                // Make sure select tags, are populated in the parsingDetails...
                //
                if (tag.equals(BATCHID))
                {
                    if (baseObject.getParsingDetailsId() == null)
                    {
                        LogLog.warn("parsing details id is null");
                        continue;
                    }

                    ParsingDetail parsingDetail = patient.getParsingDetailById(baseObject.getParsingDetailsId());

                    //
                    // If there isn't a parsing details with this id, we should create one, since it will be
                    // required to have the batchId..
                    //

                    // If all the parsing details are null, we will create a new one for each codedBaseObjects.

                    if (parsingDetail == null)
                    {
                        baseObject.setParsingDetailsId(masterParsingDetail.getParsingDetailsId());
                        String batchId = baseObject.getMetaTag(BATCHID);
                        if (StringUtils.isNotEmpty(batchId) && masterParsingDetail.getSourceUploadBatch() == null)
                        {
                            masterParsingDetail.setSourceUploadBatch(batchId);
                        }
                    }
                }

                baseObject.getMetadata().remove(tag);
            }
        }
    }

    protected void removeEventMetadata(Patient apo)
    {
        Map<String, String> metadata = apo.getMetadata();
        for(String tag: EVENTTAGS) {
            metadata.remove(tag);
        }
    }

    /**
     *
     * This will call the prerequisiteMerge, and should be overridden in the superclass
     * to provide functionality beyond what is implemented here.
     *
     * This default implementation doesn't actual perform any meaningful merge.
     *
     * @param patientSet
     * @return
     */
    protected Patient merge(PatientSet patientSet)
    {
        // Need to make sure we do these merges, since the encounter will use
        // merge down metadata that is set by these merges
        prerequisiteMerges(patientSet);
        actualMerge(patientSet);

        return patientSet.mergedPatient;
    }
}
