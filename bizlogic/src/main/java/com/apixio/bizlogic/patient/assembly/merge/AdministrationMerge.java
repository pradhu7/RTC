package com.apixio.bizlogic.patient.assembly.merge;

import java.util.Iterator;

import com.apixio.model.patient.Administration;

/**
 * Created by dyee on 5/7/17.
 */
public class AdministrationMerge
{
    public static void merge(PatientSet patientSet)
    {
        Iterable<Administration> existingAdm = patientSet.basePatient.getAdministrations();
        if (existingAdm!=null)
        {
            Iterator<Administration> it = existingAdm.iterator();
            while(it.hasNext())
            {
                patientSet.mergedPatient.addAdministration(it.next());
            }
        }

        existingAdm = patientSet.additionalPatient.getAdministrations();
        if (existingAdm!=null)
        {
            Iterator<Administration> it = existingAdm.iterator();
            while(it.hasNext())
            {
                patientSet.mergedPatient.addAdministration(it.next());
            }
        }
    }
}
