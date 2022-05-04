package com.apixio.bizlogic.patient.assembly.merge;

import java.util.HashSet;
import java.util.Set;

import com.apixio.model.patient.Apixion;

/**
 * Created by dyee on 5/9/17.
 */
public class ApixionMerge
{
    public static void merge(PatientSet patientSet)
    {
        Set<Apixion> apixionSet = new HashSet<>();

        if (patientSet.basePatient.getApixions() != null)
        {
            for (Apixion apixion : patientSet.basePatient.getApixions())
            {
                apixionSet.add(apixion);
            }
        }

        if (patientSet.additionalPatient.getApixions() != null)
        {
            for (Apixion apixion : patientSet.additionalPatient.getApixions())
            {
                apixionSet.add(apixion);
            }
        }

        patientSet.mergedPatient.setApixions(apixionSet);
    }
}
