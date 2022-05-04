package com.apixio.useracct.eprops;

import java.util.Map;

import com.apixio.restbase.eprops.E2Properties;
import com.apixio.restbase.eprops.E2Property;
import com.apixio.useracct.entity.PasswordPolicy;

public class PasswordPolicyProperties {

    /**
     * JSON field names
     */
    public static final String MAX_DAYS = "maxDays";

    /**
     * Convenience method to create a Map that can be easily translated to a JSON
     * object. 
     */
    public static Map<String, Object> toJson(PasswordPolicy policy)
    {
        Map<String, Object> json =  autoProperties.getFromEntity(policy);

        json.put("policyID", policy.getID().toString());

        return json;
    }

    /**
     * The list of automatically handled properties of an PasswordPolicy
     */
    private final static E2Properties<PasswordPolicy> autoProperties = new E2Properties<PasswordPolicy>(
        PasswordPolicy.class,
        new E2Property("name"),
        new E2Property(MAX_DAYS),
        new E2Property("minChars"),
        new E2Property("maxChars"),
        new E2Property("minLower"),
        new E2Property("minUpper"),
        new E2Property("minDigits"),
        new E2Property("minSymbols"),
        new E2Property("noReuseCount"),
        new E2Property("noUserID")
        );

}
