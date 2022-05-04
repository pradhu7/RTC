package com.apixio.bizlogic.patient.assembly.merge;

import java.util.Iterator;

import com.apixio.model.patient.SocialHistory;

/**
 * Created by dyee on 5/7/17.
 */
public class SocialHistoryMerge
{
    public static void merge(PatientSet patientSet)
    {
        Iterable<SocialHistory> existingAdm = patientSet.basePatient.getSocialHistories();
        if (existingAdm!=null)
        {
            Iterator<SocialHistory> it = existingAdm.iterator();
            while (it.hasNext())
            {
                patientSet.mergedPatient.addSocialHistory(it.next());
            }
        }

        existingAdm = patientSet.additionalPatient.getSocialHistories();
        if (existingAdm!=null)
        {
            Iterator<SocialHistory> it = existingAdm.iterator();
            while (it.hasNext())
            {
                patientSet.mergedPatient.addSocialHistory(it.next());
            }
        }
    }
}
