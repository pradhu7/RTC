package com.apixio.dao.merge;

import com.apixio.model.patient.BaseObject;
import com.apixio.model.patient.EditType;

import java.util.*;

/**
 * Created by anthony on 6/9/14.
 */
public class MappedBaseObjectMerger {

    private Map<UUID,BaseObject> mergedBaseObjectUUIDMap = new LinkedHashMap<>();
    private Map<String,BaseObject> mergedBaseObjectKeyMap = new LinkedHashMap<>();
    private Map<String,EditType> inactiveBaseObjectKeys = new HashMap<>();

    public MappedBaseObjectMerger() {

    }

    public BaseObject getBaseObjectByKey(String key) {
        return mergedBaseObjectKeyMap.get(key);
    }

    public BaseObject getBaseObjectByUUID(UUID uuid) {
        return mergedBaseObjectUUIDMap.get(uuid);
    }

    public Collection<BaseObject> getBaseObjects() {
        return mergedBaseObjectKeyMap.values();
    }

    public void addBaseObject(BaseObject baseObject) {
        if (baseObject == null) {
            return;
        }

        String key = ObjectKeys.getObjectKey(baseObject);
        BaseObject mergedBaseObject = mergedBaseObjectKeyMap.get(key);

        EditType editType = baseObject.getEditType();
        if (editType != EditType.ACTIVE) {
            // if not active, we need to retain this and wipe out anything matching this key
            // but we only need to add it once (most recent wins)
            if (!inactiveBaseObjectKeys.containsKey(key)) {
                inactiveBaseObjectKeys.put(key, editType);
            }
        } else {
            // this item may be active, but we need to check if it was subsequently archived or deleted
            if (inactiveBaseObjectKeys.containsKey(key)) {
                // is it as simple as just skipping this item entirely?
                // TODO: no, I think we have to consider the source dates
                //       unless we can guarantee objects added in chronological order...
            } else {
                // Since this is active and not deleted, add it to the existing object (if available)
                mergedBaseObject = (BaseObject) MergeHelper.mergeObject(mergedBaseObject, baseObject);
                mergedBaseObjectKeyMap.put(key, mergedBaseObject);
                mergedBaseObjectUUIDMap.put(baseObject.getInternalUUID(), mergedBaseObject);
            }
        }

    }

    public void addBaseObjects(Iterable<? extends BaseObject> baseObjectList) {
        if (baseObjectList != null)
            for (BaseObject baseObject : baseObjectList)
                addBaseObject(baseObject);
    }

    public void addBaseObjects(Map<UUID, BaseObject> baseObjectMap, List<UUID> baseObjectUUIDList) {
        for (UUID baseObjectUUID : baseObjectUUIDList) {
            addBaseObject(baseObjectMap.get(baseObjectUUID));
        }
    }
}
