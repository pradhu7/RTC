package com.apixio.bizlogic.patient.assembly.merge;

import java.util.Iterator;

import com.apixio.model.patient.FamilyHistory;

/**
 * Created by dyee on 5/7/17.
 */
public class FamilyHistoryMerge
{
    public static void merge(PatientSet patientSet)
    {
        Iterable<FamilyHistory> existingAdm = patientSet.basePatient.getFamilyHistories();
        if (existingAdm!=null)
        {
            Iterator<FamilyHistory> it = existingAdm.iterator();
            while (it.hasNext())
            {
                patientSet.mergedPatient.addFamilyHistory(it.next());
            }
        }

        existingAdm = patientSet.additionalPatient.getFamilyHistories();
        if (existingAdm!=null)
        {
            Iterator<FamilyHistory> it = existingAdm.iterator();
            while (it.hasNext())
            {
                patientSet.mergedPatient.addFamilyHistory(it.next());
            }
        }
    }
}
