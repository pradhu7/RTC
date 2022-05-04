package com.apixio.bizlogic.patient.assembly.merge;

import java.util.Iterator;

import com.apixio.model.patient.BiometricValue;

/**
 * Created by dyee on 5/7/17.
 */
public class BiometricValueMerge
{
    public static void merge(PatientSet patientSet)
    {
        Iterable<BiometricValue> existingAdm = patientSet.basePatient.getBiometricValues();
        if (existingAdm!=null)
        {
            Iterator<BiometricValue> it = existingAdm.iterator();
            while (it.hasNext())
            {
                patientSet.mergedPatient.addBiometricValue(it.next());
            }
        }

        existingAdm = patientSet.additionalPatient.getBiometricValues();
        if (existingAdm!=null)
        {
            Iterator<BiometricValue> it = existingAdm.iterator();
            while(it.hasNext())
            {
                patientSet.mergedPatient.addBiometricValue(it.next());
            }
        }
    }
}
