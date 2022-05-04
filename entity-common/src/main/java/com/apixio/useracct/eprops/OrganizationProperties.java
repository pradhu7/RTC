package com.apixio.useracct.eprops;

import java.util.Map;

import com.apixio.restbase.eprops.E2Properties;
import com.apixio.restbase.eprops.E2Property;
import com.apixio.useracct.entity.Organization;
import com.apixio.useracct.entity.OrgType;

public class OrganizationProperties {

    /**
     * Convenience method to create a Map that can be easily translated to a JSON
     * object.
     */
    public static Map<String, Object> toJson(Organization org, OrgType orgType) {
        Map<String, Object> json = autoProperties.getFromEntity(org);

        json.put("id", org.getID().toString());
        json.put("type", orgType.getName());

        return json;
    }

    /**
     * The list of automatically handled properties of an Organization.
     * <p>
     * Note: orgID and externalID are the SAME; having both is solely intended to
     * support both old (those that submit "orgID") and new ("externalID") until
     * all old clients supply externalID
     */
    private final static E2Properties<Organization> autoProperties = new E2Properties<Organization>(
            Organization.class,
            (new E2Property("orgID")).entityPropertyName("externalID").readonly(true),
            (new E2Property("externalID")).readonly(true),
            new E2Property("isActive"),
            new E2Property("name"),
            new E2Property("description"),
            new E2Property("passwordPolicy"),
            new E2Property("activityTimeoutOverride"),
            new E2Property("needsTwoFactor"),
            new E2Property("maxFailedLogins")
    );
}
