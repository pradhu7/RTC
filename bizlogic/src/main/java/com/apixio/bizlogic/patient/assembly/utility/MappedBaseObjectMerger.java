package com.apixio.bizlogic.patient.assembly.utility;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.apixio.bizlogic.patient.assembly.merge.MergeUtility;
import com.apixio.model.patient.BaseObject;
import com.apixio.model.patient.EditType;

public class MappedBaseObjectMerger
{
    private Map<UUID,BaseObject>   mergedBaseObjectUUIDMap = new LinkedHashMap<>();
    private Map<String,BaseObject> mergedBaseObjectKeyMap = new LinkedHashMap<>();
    private Map<String,EditType>   inactiveBaseObjectKeys = new HashMap<>();

    private Map<String, UUID> mergedBaseObjectKeyToAuthoritativeUUIDMap = new HashMap<>();
    private Map<UUID, UUID> mergedBaseObjectUUIDToAuthoritativeUUIDMap = new HashMap<>();

    public MappedBaseObjectMerger()
    {
    }

    public Map<UUID, UUID> getMergedBaseObjectUUIDToAuthoritativeUUIDMap()
    {
        return mergedBaseObjectUUIDToAuthoritativeUUIDMap;
    }

    public Collection<BaseObject> getBaseObjects()
    {
        return mergedBaseObjectKeyMap.values();
    }

    public void addBaseObject(BaseObject baseObject)
    {
        if (baseObject == null)
        {
            return;
        }

        String key = ObjectKeys.getObjectKey(baseObject);
        BaseObject mergedBaseObject = mergedBaseObjectKeyMap.get(key);

        EditType editType = baseObject.getEditType();
        if (editType != EditType.ACTIVE)
        {
            // if not active, we need to retain this and wipe out anything matching this key
            // but we only need to add it once (most recent wins)
            if (!inactiveBaseObjectKeys.containsKey(key))
            {
                inactiveBaseObjectKeys.put(key, editType);
            }
        }
        else
        {
            // this item may be active, but we need to check if it was subsequently archived or deleted
            if (inactiveBaseObjectKeys.containsKey(key))
            {
                // is it as simple as just skipping this item entirely?
                // TODO: no, I think we have to consider the source dates
                //       unless we can guarantee objects added in chronological order...
            }
            else
            {
                // Since this is active and not deleted, add it to the existing object (if available)
                mergedBaseObject = (BaseObject) MergeUtility.mergeObject(mergedBaseObject, baseObject);
                mergedBaseObjectKeyMap.put(key, mergedBaseObject);

                UUID baseObjectUUID = baseObject.getInternalUUID();

                //
                // Register the authoritative uuid, if it's not set already.
                //
                if(!mergedBaseObjectKeyToAuthoritativeUUIDMap.containsKey(key))
                {
                    mergedBaseObjectKeyToAuthoritativeUUIDMap.put(key, baseObjectUUID);
                }

                //
                // Add the baseObject's UUID to the mapping
                //
                UUID authUUID = mergedBaseObjectKeyToAuthoritativeUUIDMap.get(key);
                if(authUUID!=null)
                {
                    mergedBaseObjectUUIDToAuthoritativeUUIDMap.put(baseObjectUUID, authUUID);
                }

                mergedBaseObjectUUIDMap.put(baseObjectUUID, mergedBaseObject);
            }
        }

    }

    public void addBaseObjects(Iterable<? extends BaseObject> baseObjectList)
    {
        if (baseObjectList != null)
            for (BaseObject baseObject : baseObjectList)
                addBaseObject(baseObject);
    }
}
