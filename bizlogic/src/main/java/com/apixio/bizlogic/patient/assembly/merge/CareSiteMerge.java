package com.apixio.bizlogic.patient.assembly.merge;

import com.apixio.model.patient.BaseObject;
import com.apixio.model.patient.CareSite;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Created by dyee on 5/7/17.
 */
public class CareSiteMerge extends MergeBase
{
    public static final String MERGE_DOWN_MAP = "careSiteMergeDownMap";

    private static CareSiteMerge instance = new CareSiteMerge();

    public static void merge(PatientSet patientSet)
    {
        instance.mergeCareSite(patientSet);
    }

    public void mergeCareSite(PatientSet patientSet)
    {
        Map<String, UUID> mergedBaseObjectKeyToAuthoritativeUUIDMap = new HashMap<>();
        Map<UUID, UUID> mergedBaseObjectUUIDToAuthoritativeUUIDMap = new HashMap<>();

        merge(patientSet.basePatient.getCareSites(), patientSet, mergedBaseObjectKeyToAuthoritativeUUIDMap, mergedBaseObjectUUIDToAuthoritativeUUIDMap);
        merge(patientSet.additionalPatient.getCareSites(), patientSet, mergedBaseObjectKeyToAuthoritativeUUIDMap, mergedBaseObjectUUIDToAuthoritativeUUIDMap);

        patientSet.addMergeContext(MERGE_DOWN_MAP, mergedBaseObjectUUIDToAuthoritativeUUIDMap);
    }

    private void merge(Iterable<CareSite> careSites,
                              PatientSet patientSet,
                              Map<String, UUID> mergedBaseObjectKeyToAuthoritativeUUIDMap,
                              Map<UUID, UUID> mergedBaseObjectUUIDToAuthoritativeUUIDMap)
    {
        if (careSites!=null)
        {
            Iterator<CareSite> it = careSites.iterator();
            while (it.hasNext())
            {
                CareSite careSite = it.next();
                String partId = shouldProcess(careSite);
                if(!partId.isEmpty())
                {
                    UUID baseObjectUUID = careSite.getCareSiteId();

                    if (!mergedBaseObjectKeyToAuthoritativeUUIDMap.containsKey(partId))
                    {
                        reconcileReferenceSourceId(careSite, patientSet);
                        reconcileReferenceParsingDetailId(careSite, patientSet);

                        mergedBaseObjectKeyToAuthoritativeUUIDMap.put(partId, baseObjectUUID);

                        patientSet.mergedPatient.addCareSite(careSite);
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

    protected String getIdentity(BaseObject baseObject)
    {
        CareSite careSite = (CareSite)baseObject;
        StringBuilder sb = new StringBuilder();
        sb.append("careSiteName").append(careSite.getCareSiteName())
                .append("careSiteType").append(careSite.getCareSiteType() == null ? "null" : careSite.getCareSiteType().name())
                .append("address").append(careSite.getAddress() == null ? "null" : getConsistentOrderAddress(careSite.getAddress()))
                .append(getConsistentOrderedMetaData(careSite.getMetadata()));

        return sb.toString();
    }
}
