package com.apixio.dao.merge;

import java.util.*;

/**
 * Created by anthony on 6/9/14.
 */
public class MappedObjectMerger {
    private Map<String,Object> mergedObjectKeyMap = new LinkedHashMap<String, Object>();

    public MappedObjectMerger() {

    }

    public Object getObjectByKey(String key) {
        return mergedObjectKeyMap.get(key);
    }

    public Collection<Object> getObjects() {
        return mergedObjectKeyMap.values();
    }

    public void addObject(Object object) {
        if (object == null) {
            return;
        }

        String key = ObjectKeys.getObjectKey(object);
        Object mergedObject = mergedObjectKeyMap.get(key);

        mergedObject = MergeHelper.mergeObject(mergedObject, object);
        mergedObjectKeyMap.put(key, mergedObject);
    }

    public void addObjects(Iterable<? extends Object> objectList) {
        if (objectList != null)
            for (Object object : objectList)
               addObject(object);
    }
}
