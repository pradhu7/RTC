package com.apixio.useracct.dw.resources;

import com.apixio.XUUID;
import com.apixio.restbase.DaoBase;
import com.apixio.restbase.dao.CachingBase;
import com.apixio.restbase.dao.DataVersions;
import com.apixio.restbase.web.BaseRS;
import com.apixio.useracct.PrivSysServices;
import com.apixio.useracct.buslog.OrganizationLogic;
import com.apixio.useracct.dao.Organizations;
import com.apixio.useracct.dw.ServiceConfiguration;
import com.apixio.useracct.entity.OrgType;
import com.apixio.useracct.entity.Organization;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.glassfish.jersey.test.DeploymentContext;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.ServletDeploymentContext;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.api.support.membermodification.MemberMatcher;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Matchers.any;

@PowerMockIgnore("javax.management.*")
@RunWith(PowerMockRunner.class)
@PrepareForTest({OrganizationRS.class, BaseRS.class,
                OrganizationLogic.class, Organizations.class,
                DaoBase.class, DataVersions.class,
                CachingBase.class, Organization.class})
public class OrganizationRSTest extends JerseyTest
{
    public static final String VALID_XUUID = "UO_04ccec74-2959-43ea-9ab9-d4aca59f2c29";
    public static final String UORGS = "uorgs";
    private PrivSysServices sysServices;
    private OrganizationRS organizationRS;

    @Override
    protected TestContainerFactory getTestContainerFactory() {
        return new GrizzlyWebTestContainerFactory();
    }

    // Configuring a servlet for request and response
    // BatchRS service is just partially mocked by PowerMockito
    @Override
    protected DeploymentContext configureDeployment()
    {
        sysServices = PowerMockito.mock(PrivSysServices.class);
        ServiceConfiguration configuration = PowerMockito.mock(ServiceConfiguration.class);
        organizationRS = PowerMockito.spy(new OrganizationRS(configuration, sysServices));

        return ServletDeploymentContext.forServlet(
                new ServletContainer(new ResourceConfig().register(organizationRS))).build();
    }

    private void supressLogger()
    {
        PowerMockito.suppress(PowerMockito.method(BaseRS.ApiLogger.class, "addParameter", String.class, Object.class));
    }

    private Organizations partiallyMockingOrganizationLogic() throws IllegalAccessException
    {

        PowerMockito.suppress(PowerMockito.constructor(Organizations.class));
        OrganizationLogic organizationLogic  = PowerMockito.spy(new OrganizationLogic(sysServices));

        PowerMockito.when(sysServices.getOrganizationLogic()).thenReturn(organizationLogic);
        Organizations organizations = PowerMockito.spy(new Organizations(PowerMockito.mock(DaoBase.class),
                PowerMockito.mock((DataVersions.class))));

        MemberMatcher.field(OrganizationLogic.class, "organizations").set(organizationLogic, organizations);

        //Injecting customer Org Type
        OrgType customerOrgType = PowerMockito.mock(OrgType.class);
        MemberMatcher.field(OrganizationLogic.class, "customerOrgType").set(organizationLogic, customerOrgType);
        PowerMockito.when(customerOrgType.getID()).thenReturn(XUUID.fromString(VALID_XUUID));

        return organizations;
    }

    /**
     * GET	/uorgs/{orgID}	Invalid UUID string:
     */
    @Test
    public void getUorgWithInvalidXuuid() throws IllegalAccessException
    {
        String invalidOrgId = "invalid";

        supressLogger();
        partiallyMockingOrganizationLogic();

        Response response = target()
                .path(UORGS).path("{orgID}")
                .resolveTemplate("orgID", invalidOrgId)
                .request()
                .get();

        Assert.assertEquals(response.getStatus(), 400);
    }

    /**
     * GET	/uorgs/{orgID}/members	Invalid UUID string:
     **/
    @Test
    public void getUorgMembersWithInvaildIdType() throws IllegalAccessException
    {
        String invalidIdType = "O_04ddef74-2959-43ea-9ab9-d4aca59f2c29";

        supressLogger();
        partiallyMockingOrganizationLogic();

        Response response = target()
                .path(UORGS).path("{orgID}").path("members")
                .resolveTemplate("orgID", invalidIdType)
                .request()
                .get();

        Assert.assertEquals(response.getStatus(), 400);
    }

    /**
     * GET	/uorgs/{orgID}/patientdatasets	Attempt to get Organization PatientDataSet list on a non-customer Organization
     **/
    @Test
    public void getUorgPatientDataSetsWithIncorrectID() throws IllegalAccessException
    {
        String origID = "UO_04ddec74-2959-43ea-9ab9-d4aca59f2c29";

        supressLogger();
        Organizations organizations = partiallyMockingOrganizationLogic();

        Organization org = PowerMockito.mock(Organization.class);
        PowerMockito.doReturn(org).when(organizations).findOrganizationByID(any(XUUID.class));
        PowerMockito.when(org.getOrgType()).thenReturn(XUUID.fromString(origID));

        Response response = target()
                .path(UORGS).path("{orgID}").path("patientdatasets")
                .resolveTemplate("orgID", origID)
                .request()
                .get();

        Assert.assertEquals(response.getStatus(), 400);
    }

    /**
     * GET	/uorgs/{orgID}/patientdatasets	Invalid UUID string:
     **/
    @Test
    public void getUorgPatientDataSetsWithInvalidIDType() throws IllegalAccessException
    {
        String origID = "invalid";

        supressLogger();
        partiallyMockingOrganizationLogic();

        Response response = target()
                .path(UORGS).path("{orgID}").path("patientdatasets")
                .resolveTemplate("orgID", origID)
                .request()
                .get();

        Assert.assertEquals(response.getStatus(), 400);
    }

    /**
     * PUT	/uorgs/{orgID}/properties/{name}	Invalid UUID string:
     **/
    @Test
    public void putPropertyWithInvalidUorgID() throws IllegalAccessException {
        String invalidOrgID = "invalid";
        String propertyName = "orgtype";
        String value = "Vendor";
        Map<String,String> parms = new HashMap<>();
        parms.put("value", value);

        supressLogger();
        partiallyMockingOrganizationLogic();

        Response response = target()
                .path(UORGS).path("{orgID}").path("properties").path("{name}")
                .resolveTemplate("orgID", invalidOrgID)
                .resolveTemplate("name", propertyName)
                .request()
                .put(Entity.json(parms));

        Assert.assertEquals(response.getStatus(), 400);
    }
}
