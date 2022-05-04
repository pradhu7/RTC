package com.apixio.useracct.dw.resources;

import org.glassfish.jersey.client.JerseyClientBuilder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.servlet.ServletContainer;
import org.glassfish.jersey.test.DeploymentContext;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.ServletDeploymentContext;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class OrganizationRSResourceTest extends JerseyTest
{
    private static Logger LOGGER = LoggerFactory.getLogger(OrganizationRSResourceTest.class);
    private static Client client = null;

    static
    {
        client = new JerseyClientBuilder().build();
        BaseResourceTest.mainSetup();
    }

    @Override
    protected TestContainerFactory getTestContainerFactory() {
        return new GrizzlyWebTestContainerFactory();
    }

    @Override
    protected DeploymentContext configureDeployment()
    {
        ResourceConfig resourceConfig = new ResourceConfig()
                .register(new OrganizationRS(BaseResourceTest.serviceConfiguration, BaseResourceTest.sysServices));
        Map<String, Object> props = new HashMap<>();
        props.put(ServerProperties.RESPONSE_SET_STATUS_OVER_SEND_ERROR, "true");
        resourceConfig.addProperties(props);
        return ServletDeploymentContext.forServlet(
                new ServletContainer(resourceConfig)).build();
    }

    private Params createOrg(Map<String, String> createParams)
    {
        Response response = target().path("uorgs")
                .request()
                .post(Entity.json(createParams));
        Assert.assertEquals(response.getStatus(),200);

        return response.readEntity(Params.class);
    }

    private void deleteOrg(String orgID) throws IOException
    {
        Response response = target().path("uorgs").path("{orgID}")
                .resolveTemplate("orgID", orgID)
                .request()
                .delete();

        Assert.assertEquals(response.getStatus(),200);

    }

    private Params getOrg(String orgID)
    {
        Response response = target().path("uorgs").path("{orgID}")
                .resolveTemplate("orgID", orgID)
                .request()
                .get();
        Assert.assertEquals(response.getStatus(), 200);

        return response.readEntity(Params.class);
    }

    private void putOrg(String orgID, Map<String, String> updateParams)
    {
        Response response = target().path("uorgs").path("{orgID}")
                .resolveTemplate("orgID", orgID)
                .request()
                .put(Entity.json(updateParams));

        Assert.assertEquals(response.getStatus(), 200);
    }

    public static class Params {
        public String name;
        public String description;
        public String type;
        public String id;
        public Boolean isActive;
        public Boolean needsTwoFactor;
        public String[] ipWhitelist;
        public String passwordPolicy;
        public Integer activityTimeoutOverride;
        public Integer  maxFailedLogins;


        @Override
        public String toString() {
            return "Params{" +
                    "name='" + name + '\'' +
                    ", description='" + description + '\'' +
                    ", type='" + type + '\'' +
                    ", id='" + id + '\'' +
                    ", isActive=" + isActive +
                    ", needsTwoFactor=" + needsTwoFactor +
                    ", ipWhitelist=" + Arrays.toString(ipWhitelist) +
                    ", passwordPolicy='" + passwordPolicy + '\'' +
                    ", activityTimeoutOverride=" + activityTimeoutOverride +
                    ", maxFailedLogins=" + maxFailedLogins +
                    '}';
        }
    }

    @Test
    public void createOrgsTest() throws IOException
    {
        Map<String, String> createParams = new HashMap<>();
        createParams.put("type", "Vendor");
        createParams.put("needsTwoFactor", "true");
        createParams.put("name", "TestOrg");
        createParams.put("description", "whatever");

        Params respParams = createOrg(createParams);

        LOGGER.info("Created Parameters : " + respParams.toString());

        //Verify params
        Assert.assertEquals(createParams.get("type"), respParams.type);
        Assert.assertEquals(createParams.get("needsTwoFactor"), respParams.needsTwoFactor.toString());
        Assert.assertEquals(createParams.get("name"), respParams.name);
        Assert.assertEquals(true, respParams.isActive);

        deleteOrg(respParams.id);
    }

    @Test
    public void createOrgWithMaxFailedLoginLimitTest() throws IOException
    {
        Map<String, String> createParams = new HashMap<>();
        createParams.put("type", "Vendor");
        createParams.put("needsTwoFactor", "true");
        createParams.put("name", "TestOrg");
        createParams.put("description", "whatever");
        createParams.put("maxFailedLogins", "3");

        Params respParams = createOrg(createParams);

        LOGGER.info("Created Parameters : " + respParams.toString());

        //Verify params
        Assert.assertEquals(createParams.get("type"), respParams.type);
        Assert.assertEquals(createParams.get("needsTwoFactor"), respParams.needsTwoFactor.toString());
        Assert.assertEquals(createParams.get("name"), respParams.name);
        Assert.assertEquals(true, respParams.isActive);
        Assert.assertEquals(3, respParams.maxFailedLogins.intValue());

        deleteOrg(respParams.id);
    }

    @Test
    public void createOrgsTestWithoutTwoFactor() throws IOException
    {
        Map<String, String> createParams = new HashMap<>();
        createParams.put("type", "Vendor");
        createParams.put("name", "TestOrg");
        createParams.put("description", "whatever");

        Params respParams = createOrg(createParams);

        LOGGER.info("Created Parameters : " + respParams.toString());

        //Verify params
        Assert.assertEquals(createParams.get("type"), respParams.type);
        Assert.assertEquals(false, respParams.needsTwoFactor);
        Assert.assertEquals(createParams.get("name"), respParams.name);
        Assert.assertEquals(true, respParams.isActive);

        // clean up
        deleteOrg(respParams.id);
    }

    @Test
    public void validateFieldsForGetOrgRequest() throws IOException {
        Map<String, String> createParams = new HashMap<>();
        createParams.put("type", "Vendor");
        createParams.put("needsTwoFactor", "true");
        createParams.put("name", "TestOrg");
        createParams.put("description", "whatever");

        Params respParams = createOrg(createParams);

        LOGGER.info("Created Parameters : " + respParams.toString());

        Params params = getOrg(respParams.id);

        //Verify params
        Assert.assertEquals(createParams.get("type"), params.type);
        Assert.assertEquals(createParams.get("needsTwoFactor"), params.needsTwoFactor.toString());
        Assert.assertEquals(createParams.get("name"), params.name);
        Assert.assertEquals(true, params.isActive);

        // clean up
        deleteOrg(params.id);
    }

    @Test
    public void updateOrgFields() throws IOException
    {
        Map<String, String> createParams = new HashMap<>();
        createParams.put("type", "Customer");
        createParams.put("needsTwoFactor", "false");
        createParams.put("name", "TestOrg");
        createParams.put("description", "whatever");

        Params respParams = createOrg(createParams);

        LOGGER.info("Created Params : " + respParams);

        Map<String,String> updateParams = new HashMap<>();
        updateParams.put("needsTwoFactor", "true");
        updateParams.put("maxFailedLogins", "4");
        putOrg(respParams.id, updateParams);

        Params params = getOrg(respParams.id);

        //Verify params
        Assert.assertEquals(createParams.get("type"), params.type);
        Assert.assertEquals(true, params.needsTwoFactor);
        Assert.assertEquals(createParams.get("name"), params.name);
        Assert.assertEquals(true, params.isActive);
        Assert.assertNotNull(params.maxFailedLogins);
        Assert.assertTrue(params.maxFailedLogins.equals(4));

        // clean up
        deleteOrg(params.id);
    }

}