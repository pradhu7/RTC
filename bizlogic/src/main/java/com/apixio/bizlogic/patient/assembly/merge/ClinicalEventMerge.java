package com.apixio.bizlogic.patient.assembly.merge;

import java.util.Iterator;

import com.apixio.model.patient.event.Event;

/**
 * Created by dyee on 5/7/17.
 */
public class ClinicalEventMerge
{
    public static void merge(PatientSet patientSet)
    {
        Iterable<Event> existingAdm = patientSet.basePatient.getClinicalEvents();
        if (existingAdm!=null)
        {
            Iterator<Event> it = existingAdm.iterator();
            while (it.hasNext())
            {
                patientSet.mergedPatient.addClinicalEvent(it.next());
            }
        }

        existingAdm = patientSet.additionalPatient.getClinicalEvents();
        if (existingAdm!=null)
        {
            Iterator<Event> it = existingAdm.iterator();
            while (it.hasNext())
            {
                patientSet.mergedPatient.addClinicalEvent(it.next());
            }
        }
    }
}
