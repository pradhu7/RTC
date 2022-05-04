package com.apixio.useracct.eprops;

import java.util.Map;

import com.apixio.restbase.eprops.E2Properties;
import com.apixio.restbase.eprops.E2Property;
import com.apixio.useracct.entity.PatientDataSet;

public class PdsProperties {

    /**
     * Convenience method to create a Map that can be easily translated to a JSON
     * object. 
     */
    public static Map<String, Object> toJson(PatientDataSet pds)
    {
        Map<String, Object> json =  autoProperties.getFromEntity(pds);

        json.put("id", pds.getID().toString());

        return json;
    }

    /**
     * The list of automatically handled properties of an Pds
     */
    private final static E2Properties<PatientDataSet> autoProperties = new E2Properties<>(PatientDataSet.class,
        new E2Property("name"),
        new E2Property("description"),
        (new E2Property("externalID")).entityPropertyName("COID").readonly(true),
        (new E2Property("coID")).entityPropertyName("COID").readonly(true),
        (new E2Property("isActive")).entityPropertyName("active").readonly(true),
        (new E2Property("ownerOrg")).entityPropertyName("owningOrganization").readonly(true)
        );

}
