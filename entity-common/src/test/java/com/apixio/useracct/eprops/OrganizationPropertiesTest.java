package com.apixio.useracct.eprops;

import com.apixio.restbase.entity.ParamSet;
import com.apixio.useracct.entity.OrgType;
import com.apixio.useracct.entity.Organization;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;
import java.util.UUID;

public class OrganizationPropertiesTest {

    @Test
    public void ParseOrganizationProperties() {
        Organization org =
                new Organization(
                        new ParamSet("{\"id\":\""
                                + UUID.randomUUID().toString()
                                + "\",\"name\":\"name\",\"description\":\"description\",\"created-at\":\"1617724800\"}")
                );

        Map<String, Object> json = OrganizationProperties.toJson(org, new OrgType("test"));

        Assert.assertNotNull(json);

    }

    @Test
    public void ParseOrganizationPropertiesMaxFailedLogins() {
        Organization org =
                new Organization(
                        new ParamSet("{\"id\":\""
                                + UUID.randomUUID().toString()
                                + "\",\"name\":\"name\",\"description\":\"description\",\"created-at\":\"1617724800\","
                                + "\"max-failed-logins\":\"4\"}")
                );

        Map<String, Object> json = OrganizationProperties.toJson(org, new OrgType("test"));

        Assert.assertNotNull(json);
        Assert.assertTrue(json.containsKey("maxFailedLogins"));
    }
}
