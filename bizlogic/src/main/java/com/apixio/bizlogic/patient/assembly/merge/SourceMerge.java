package com.apixio.bizlogic.patient.assembly.merge;

import com.apixio.model.patient.BaseObject;
import com.apixio.model.patient.Source;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Created by dyee on 10/27/17.
 */

public class SourceMerge extends MergeBase
{
    public static final String MERGE_DOWN_MAP = "sourceMergeDownMap";

    private static SourceMerge instance = new SourceMerge();

    public static void merge(PatientSet patientSet)
    {
        instance.mergeSource(patientSet);
    }

    public void mergeSource(PatientSet patientSet)
    {
        Map<String, UUID> mergedBaseObjectKeyToAuthoritativeUUIDMap = new HashMap<>();
        Map<UUID, UUID> mergedBaseObjectUUIDToAuthoritativeUUIDMap = new HashMap<>();

        merge(patientSet.basePatient.getSources(), patientSet, mergedBaseObjectKeyToAuthoritativeUUIDMap, mergedBaseObjectUUIDToAuthoritativeUUIDMap);
        merge(patientSet.additionalPatient.getSources(), patientSet, mergedBaseObjectKeyToAuthoritativeUUIDMap, mergedBaseObjectUUIDToAuthoritativeUUIDMap);

        patientSet.addMergeContext(MERGE_DOWN_MAP, mergedBaseObjectUUIDToAuthoritativeUUIDMap);
    }

    private void merge(Iterable<Source> sources,
                       PatientSet patientSet,
                       Map<String, UUID> mergedBaseObjectKeyToAuthoritativeUUIDMap,
                       Map<UUID, UUID> mergedBaseObjectUUIDToAuthoritativeUUIDMap)
    {
        if (sources !=null)
        {
            Iterator<Source> it = sources.iterator();
            while (it.hasNext())
            {
                Source source = it.next();
                String partId = shouldProcess(source);
                if(!partId.isEmpty())
                {
                    UUID baseObjectUUID = source.getInternalUUID();
                    if (!mergedBaseObjectKeyToAuthoritativeUUIDMap.containsKey(partId))
                    {
                        mergedBaseObjectKeyToAuthoritativeUUIDMap.put(partId, baseObjectUUID);

                        patientSet.mergedPatient.addSource(source);
                    }

                    UUID authUUID = mergedBaseObjectKeyToAuthoritativeUUIDMap.get(partId);
                    if (authUUID != null)
                    {
                        mergedBaseObjectUUIDToAuthoritativeUUIDMap.put(baseObjectUUID, authUUID);
                    }
                }
            }
        }
    }

    @Override
    protected String getIdentity(BaseObject baseObject)
    {
        Source source = (Source)baseObject;
        StringBuilder builder = new StringBuilder();
        builder.append("sourceSystem")
                .append(source.getSourceSystem())
                .append("sourceType")
                .append(source.getSourceType())
                .append(getConsistentOrderedMetaData(source.getMetadata()));

        if (source.getClinicalActorId() != null)
            builder.append("clinicalActorId").append(source.getClinicalActorId().toString());

        if (source.getOrganization() != null)
            builder.append("organization").append(source.getOrganization().getName());

        if (source.getCreationDate() != null)
            builder.append("creationDate").append(source.getCreationDate().toDate().getTime());

        if (source.getDciStart() != null)
            builder.append("dciStart").append(source.getDciStart().toDate().getTime());

        if (source.getDciEnd() != null)
            builder.append("dciEnd").append(source.getDciEnd().toDate().getTime());

        return builder.toString();
    }
}
