package com.apixio.bizlogic.patient.assembly;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.apixio.bizlogic.patient.assembly.allergy.AllergyAssembler;
import com.apixio.bizlogic.patient.assembly.familyhistory.FamilyHistoryAssembler;
import com.apixio.bizlogic.patient.assembly.immunization.ImmunizationAssembler;
import com.apixio.bizlogic.patient.assembly.labresult.LabResultAssembler;
import com.apixio.bizlogic.patient.assembly.merge.AllMerge;
import com.apixio.bizlogic.patient.assembly.merge.PatientSet;
import com.apixio.bizlogic.patient.assembly.prescription.PrescriptionAssembler;
import com.apixio.bizlogic.patient.assembly.problem.ProblemAssembler;
import com.apixio.bizlogic.patient.assembly.clinicalactor.ClinicalActorAssembler;
import com.apixio.bizlogic.patient.assembly.coverage.CoverageAssembler;
import com.apixio.bizlogic.patient.assembly.demographic.DemographicAssembler;
import com.apixio.bizlogic.patient.assembly.documentMeta.DocumentMetaAssembler;
import com.apixio.bizlogic.patient.assembly.encounter.EncounterAssembler;
import com.apixio.bizlogic.patient.assembly.all.AllAssembler;
import com.apixio.bizlogic.patient.assembly.procedure.ProcedureAssembler;
import com.apixio.bizlogic.patient.assembly.socialhistory.SocialHistoryAssembler;
import com.apixio.bizlogic.patient.assembly.vitalsign.VitalSignAssembler;
import com.apixio.model.patient.Patient;
import com.apixio.model.assembly.Assembler;
import com.apixio.model.assembly.Assembly;
import com.apixio.model.patient.Problem;
import com.apixio.nassembly.patientcategory.*;

import static com.apixio.bizlogic.patient.assembly.PatientAssembly.PatientCategory.ALL;
import static com.apixio.bizlogic.patient.assembly.PatientAssembly.PatientCategory.PROBLEM;

public class PatientAssembly implements Assembly<Patient,Patient>
{
    static ConcurrentMap<String, Assembler> categoryToAssembler = new ConcurrentHashMap();

    static public String SPECIALIZATION_TAG_PREFIX = "__s__";
    static public String CLEAN_RAPS_SPECIALIZATION_TAG = SPECIALIZATION_TAG_PREFIX + "cleanProblems";

    public enum PatientCategory
    {
        //TODO: We plan on persisting clinical actor WITHOUT a patientId
        CLINICAL_ACTOR("clinicalActor", new ClinicalActorTranslator(), true),
        COVERAGE("coverage", new CoverageTranslator(), true),
        DEMOGRAPHIC("demographic", new DemographicTranslator(), false),
        DOCUMENT_META("documentMeta", new DocumentMetaTranslator(), true),
        ENCOUNTER("encounter", new EncounterTranslator(), true),
        PROBLEM("problem", new ProblemTranslator(), false),
        PROCEDURE("procedure", new ProcedureTranslator(), false),
        PRESCRIPTION("prescription", new PrescriptionTranslator(), false),
        LAB_RESULT("labResult", new LabResultTranslator(), false),
        VITAL_SIGN("vitalSign", new VitalSignTranslator(), false),
        ALLERGY("allergy", new AllergyTranslator(), false),
        IMMUNIZATION("immunization", new ImmunizationTranslator(), false),
        FAMILY_HISTORY("familyHistory", new FamilyHistoryTranslator(), false),
        SOCIAL_HISTORY("socialHistory", new SocialHistoryTranslator(), false),
        ALL("all", new AllTranslator(), true);

        private String  category;
        private PatientCategoryTranslator translator;
        private boolean hasParts;

        PatientCategory(String category, PatientCategoryTranslator translator, boolean hasParts)
        {
            this.category = category;
            this.translator = translator;
            this.hasParts = hasParts;
        }

        public String getCategory()
        {
            return category;
        }

        public PatientCategoryTranslator getTranslator()
        {
            return translator;
        }

        public boolean hasParts()
        {
            return hasParts;
        }

        public static PatientCategory fromCategory(String category)
        {
            for (PatientCategory v : values())
            {
                if (v.getCategory().equalsIgnoreCase(category))
                {
                    return v;
                }
            }

            throw new IllegalArgumentException("Wrong category for " + category);
        }
    }

    public enum SpecializedPatientCategory
    {
        PROBLEM_CLEAN_PROBLEMS(PROBLEM.category + CLEAN_RAPS_SPECIALIZATION_TAG, false),
        ALL_CLEAN_PROBLEMS(ALL.category + CLEAN_RAPS_SPECIALIZATION_TAG, true);

        private String  category;
        private boolean hasParts;

        SpecializedPatientCategory(String category, boolean hasParts)
        {
            this.category = category;
            this.hasParts = hasParts;
        }

        public String getCategory()
        {
            return category;
        }

        public boolean hasParts()
        {
            return hasParts;
        }
    }

    @Override
    public String getID(Patient patient)
    {
        return patient.getPatientId().toString();
    }

    @Override
    public Assembler<Patient, Patient> getAssembler(String category)
    {
        Assembler<Patient, Patient> assembler = categoryToAssembler.get(category);
        if (assembler == null)
        {
            assembler = createAssembler(category);
            categoryToAssembler.put(category, assembler);
        }

        return assembler;
    }

    private static Assembler<Patient, Patient> createAssembler(String category)
    {
        //
        // ------------------ REGISTER SPECIALIZATION ASSEMBLERS -------------------
        //
        // There are currently two types:
        //     1) All Assembler specialized to deal with Clean up of RAPS data
        //     2) Problem Assembler specialized to deal with Clean up of RAPS data
        //

        if (category.equals(SpecializedPatientCategory.ALL_CLEAN_PROBLEMS.getCategory()))
        {
            //
            // Return a new AllAssembler, that "merges" out problems
            //
            return new AllAssembler()
            {
                @Override
                public void actualMerge(PatientSet patientSet)
                {
                    AllMerge.merge(patientSet);
                    removeEventMetadata(patientSet.mergedPatient);

                    //remove problems
                    patientSet.mergedPatient.setProblems(new ArrayList<Problem>());
                }
            };
        }

        if (category.equals(SpecializedPatientCategory.PROBLEM_CLEAN_PROBLEMS.getCategory()))
        {
            //
            // A new problems assembler, that "merges" out problems
            //
            return new ProblemAssembler()
            {
                @Override
                public void actualMerge(PatientSet patientSet)
                {
                    //Do not preform a merge. The patientSet's mergedPatient, will be empty.
                }

                @Override
                public void prerequisiteMerges(PatientSet patientSet) {
                    // Since we're not performing a merge, no prerequiste mergeing required.
                }
            };
        }

        category = category.split(SPECIALIZATION_TAG_PREFIX)[0];

        //
        // If we get here, this means we don't have any specializations defined.. In this case,
        // default to the non specialized assemblier
        //

        if (category.equals(PatientCategory.CLINICAL_ACTOR.getCategory()))
            return new ClinicalActorAssembler();

        if (category.equals(PatientCategory.COVERAGE.getCategory()))
            return new CoverageAssembler();

        if (category.equals(PatientCategory.DEMOGRAPHIC.getCategory()))
            return new DemographicAssembler();

        if (category.equals(PatientCategory.DOCUMENT_META.getCategory()))
            return new DocumentMetaAssembler();

        if (category.equals(PatientCategory.ENCOUNTER.getCategory()))
            return new EncounterAssembler();

        if (category.equals(PROBLEM.getCategory()))
            return new ProblemAssembler();

        if (category.equals(PatientCategory.PROCEDURE.getCategory()))
            return new ProcedureAssembler();

        if (category.equals(PatientCategory.PRESCRIPTION.getCategory()))
            return new PrescriptionAssembler();

        if (category.equals(PatientCategory.LAB_RESULT.getCategory()))
            return new LabResultAssembler();

        if (category.equals(PatientCategory.VITAL_SIGN.getCategory()))
            return new VitalSignAssembler();

        if (category.equals(PatientCategory.ALLERGY.getCategory()))
            return new AllergyAssembler();

        if (category.equals(PatientCategory.IMMUNIZATION.getCategory()))
            return new ImmunizationAssembler();

        if (category.equals(PatientCategory.FAMILY_HISTORY.getCategory()))
            return new FamilyHistoryAssembler();

        if (category.equals(PatientCategory.SOCIAL_HISTORY.getCategory()))
            return new SocialHistoryAssembler();

        if (category.equals(ALL.getCategory()))
            return new AllAssembler();


        throw new IllegalArgumentException("class of type category [" + category + "] not implemented");
    }

    @Override
    public Patient combine(List<Patient> patients)
    {
        if (patients.isEmpty())
            return null;

        Patient mergedPatient = patients.get(0);

        for (int index = 1; index < patients.size(); index++)
        {
            PatientSet patientSet = new PatientSet(mergedPatient, patients.get(index));

            AllMerge.merge(patientSet);

            mergedPatient = patientSet.mergedPatient;
        }

        return mergedPatient;
    }
}
