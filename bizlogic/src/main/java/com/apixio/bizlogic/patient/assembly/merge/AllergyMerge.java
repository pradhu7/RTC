package com.apixio.bizlogic.patient.assembly.merge;

import java.util.Iterator;

import com.apixio.model.patient.Allergy;

/**
 * Created by dyee on 5/7/17.
 */
public class AllergyMerge
{
    public static void merge(PatientSet patientSet)
    {
        Iterable<Allergy> existingAdm = patientSet.basePatient.getAllergies();
        if (existingAdm!=null)
        {
            Iterator<Allergy> it = existingAdm.iterator();
            while (it.hasNext())
            {
                patientSet.mergedPatient.addAllergy(it.next());
            }
        }

        existingAdm = patientSet.additionalPatient.getAllergies();
        if (existingAdm!=null)
        {
            Iterator<Allergy> it = existingAdm.iterator();
            while (it.hasNext())
            {
                patientSet.mergedPatient.addAllergy(it.next());
            }
        }
    }
}
