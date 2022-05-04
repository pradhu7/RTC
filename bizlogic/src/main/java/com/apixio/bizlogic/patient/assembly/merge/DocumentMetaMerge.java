package com.apixio.bizlogic.patient.assembly.merge;

import com.apixio.bizlogic.patient.assembly.utility.AssemblerUtility;
import com.apixio.bizlogic.patient.assembly.utility.MappedBaseObjectMerger;
import com.apixio.model.patient.BaseObject;
import com.apixio.model.patient.Document;
import com.apixio.model.patient.Patient;
import org.joda.time.DateTime;

public class DocumentMetaMerge extends MergeBase
{
    public static void mergeDocuments(PatientSet patientSet)
    {
        //Documents can potentially be ordered by ocr timestamp
        orderPatients(patientSet);

        MappedBaseObjectMerger documentMerger = new MappedBaseObjectMerger();
        documentMerger.addBaseObjects(patientSet.basePatient.getDocuments());
        documentMerger.addBaseObjects(patientSet.additionalPatient.getDocuments());

        for (BaseObject documentObject : documentMerger.getBaseObjects())
        {
            Document d = reconcileReferences((Document) documentObject, patientSet);
            patientSet.mergedPatient.addDocument(d);

            // Copy over the references... this needs to happen *AFTER* the reconcile..
            AssemblerUtility.addClinicalActors(d, patientSet.basePatient, patientSet.mergedPatient);
            AssemblerUtility.addClinicalActors(d, patientSet.additionalPatient, patientSet.mergedPatient);

            AssemblerUtility.addEncounters(d, patientSet.basePatient, patientSet.mergedPatient);
            AssemblerUtility.addEncounters(d, patientSet.additionalPatient, patientSet.mergedPatient);

            AssemblerUtility.addParsingDetails(d, patientSet.basePatient, patientSet.mergedPatient);
            AssemblerUtility.addParsingDetails(d, patientSet.additionalPatient, patientSet.mergedPatient);

            AssemblerUtility.addSources(d, patientSet.basePatient, patientSet.mergedPatient);
            AssemblerUtility.addSources(d, patientSet.additionalPatient, patientSet.mergedPatient);
        }
    }

    /**
     * Documents can be ordered by ocr TS.
     *
     * @param patientSet
     */
    private static void orderPatients(PatientSet patientSet)
    {
        //
        // Get the latest timestamp from OCR metadata
        //
        DateTime patient1SourceDate = AssemblerUtility.getLatestSourceDateFromOcr(patientSet.basePatient);
        DateTime patient2SourceDate = AssemblerUtility.getLatestSourceDateFromOcr(patientSet.additionalPatient);

        //
        // Change ordering with regards to OCR timestamp.
        //
        if (patient1SourceDate.isEqual(patient2SourceDate) || patient1SourceDate.isAfter(patient2SourceDate))
        {
            //ignore: do nothing... don't change order.
        }
        else
        {
            reversePatient(patientSet);
        }

    }

    private static void reversePatient(PatientSet patientSet)
    {
        Patient tmpPatient = patientSet.basePatient;
        patientSet.basePatient       = patientSet.additionalPatient;
        patientSet.additionalPatient = tmpPatient;
    }

    private static Document reconcileReferences(Document documentObject, PatientSet patientSet)
    {
        if (documentObject != null)
        {
            //Order is important, reconcile must occur before addXXXX()
            reconcileReferenceParsingDetailId(documentObject, patientSet);
            reconcileReferenceSourceId(documentObject, patientSet);
            reconcileReferenceClinicalActorId(documentObject, patientSet);
            reconcileReferenceEncounterId(documentObject, patientSet);
        }
        return documentObject;
    }
}
