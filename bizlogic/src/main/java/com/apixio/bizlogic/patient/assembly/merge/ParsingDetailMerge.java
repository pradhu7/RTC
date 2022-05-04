package com.apixio.bizlogic.patient.assembly.merge;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import com.apixio.model.patient.ParsingDetail;
import org.apache.log4j.helpers.LogLog;

/**
 * Created by dyee on 5/7/17.
 */
public class ParsingDetailMerge
{
    public static void merge(PatientSet patientSet)
    {
        Map<UUID, String> seenMap = new HashMap<>();

        merge(patientSet.basePatient.getParsingDetails(), patientSet, seenMap);
        merge(patientSet.additionalPatient.getParsingDetails(), patientSet, seenMap);
    }

    private static void merge(Iterable<ParsingDetail> patientParsingDetails,
                              PatientSet patientSet, Map<UUID, String> seenMap)
    {
        if (patientParsingDetails!=null)
        {
            Iterator<ParsingDetail> it = patientParsingDetails.iterator();
            while (it.hasNext())
            {
                ParsingDetail parsingDetail = it.next();

                UUID parsingDetailsId = parsingDetail.getParsingDetailsId();

                if(parsingDetailsId==null)
                {
                    LogLog.warn("parsing details does not have an id");
                    continue;
                }

                if (!seenMap.containsKey(parsingDetailsId) &&
                     patientSet.mergedPatient.getParsingDetailById(parsingDetailsId)==null)
                {
                    patientSet.mergedPatient.addParsingDetail(parsingDetail);
                    seenMap.put(parsingDetailsId, "");
                }
            }
        }
    }
}
