package com.apixio.bizlogic.patient.assembly.merge;

public class AllMerge
{
    public static void merge(PatientSet patientSet)
    {
        //
        // These merges must occur first - since they might dedup, and we rely on these
        // merges happening first.
        //
        ParsingDetailMerge.merge(patientSet);

        //
        // Coverage Merge Handles the merging and deduping of sources
        //
        if (CoverageMerge.hasCoverage(patientSet.basePatient) || CoverageMerge.hasCoverage(patientSet.additionalPatient))
        {
            CoverageMerge.mergeCoverage(patientSet, true);
        }
        else
        {
            //
            // If we got here, this means coverage merge did not occur, and we should copy over
            // sources ourselves
            //
            SourceMerge.merge(patientSet);
        }

        // Demographic merge does also contact and meta data merge so it is not necessary to do those merges again
        DemographicMerge.mergeDemographics(patientSet);

        CareSiteMerge.merge(patientSet);
        ClinicalActorMerge.mergeClinicalActors(patientSet);
        EncounterMerge.mergeEncounters(patientSet);
        ProblemMerge.merge(patientSet);
        ProcedureMerge.merge(patientSet);
        PrescriptionMerge.merge(patientSet);
        AdministrationMerge.merge(patientSet);
        LabResultMerge.merge(patientSet);
        BiometricValueMerge.merge(patientSet);
        AllergyMerge.merge(patientSet);
        FamilyHistoryMerge.merge(patientSet);
        SocialHistoryMerge.merge(patientSet);
        ClinicalEventMerge.merge(patientSet);

        ApixionMerge.merge(patientSet);

        // IMPORTANT: Keep it the last merge since it reorders object based on ocr source date
        DocumentMetaMerge.mergeDocuments(patientSet);
    }
}
