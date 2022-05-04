package com.apixio.bizlogic.patient.assembly.utility;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import com.apixio.bizlogic.patient.assembly.merge.MergeUtility;

public class MappedObjectMerger
{
    private Map<String,Object> mergedObjectKeyMap = new LinkedHashMap<>();

    public MappedObjectMerger()
    {

    }

    public Object getObjectByKey(String key)
    {
        return mergedObjectKeyMap.get(key);
    }

    public Collection<Object> getObjects()
    {
        return mergedObjectKeyMap.values();
    }

    public void addObject(Object object)
    {
        if (object == null)
        {
            return;
        }

        String key          = ObjectKeys.getObjectKey(object);
        Object mergedObject = mergedObjectKeyMap.get(key);

        mergedObject = MergeUtility.mergeObject(mergedObject, object);
        mergedObjectKeyMap.put(key, mergedObject);
    }

    public void addObjects(Iterable<? extends Object> objectList)
    {
        if (objectList != null)
            for (Object object : objectList)
               addObject(object);
    }
}
