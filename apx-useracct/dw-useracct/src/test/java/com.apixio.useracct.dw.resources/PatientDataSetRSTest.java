package com.apixio.useracct.dw.resources;

import com.apixio.restbase.web.BaseRS;
import com.apixio.useracct.PrivSysServices;
import com.apixio.useracct.dw.ServiceConfiguration;
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
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

@PowerMockIgnore("javax.management.*")
@RunWith(PowerMockRunner.class)
@PrepareForTest({PatientDataSetRS.class})
public class PatientDataSetRSTest extends JerseyTest
{
    public static final String PATIENTDATASETS = "patientdatasets";
    private PrivSysServices sysServices;


    @Override
    protected TestContainerFactory getTestContainerFactory() {
        return new GrizzlyWebTestContainerFactory();
    }

    // Configuring a servlet for request and response
    // PatientDataSetRS service is just partially mocked by PowerMockito
    @Override
    protected DeploymentContext configureDeployment() {
        sysServices = PowerMockito.mock(PrivSysServices.class);
        ServiceConfiguration configuration = PowerMockito.mock(ServiceConfiguration.class);
        PatientDataSetRS patientDataSetRS = PowerMockito.spy(new PatientDataSetRS(configuration, sysServices));

        return ServletDeploymentContext.forServlet(
                new ServletContainer(new ResourceConfig().register(patientDataSetRS))).build();
    }

    private void supressLogger() {
        PowerMockito.suppress(PowerMockito.method(BaseRS.ApiLogger.class, "addParameter", String.class, Object.class));
    }

    // when the PDS ID is invalid
    // GET	/patientdatasets/{pdsID}	Invalid UUID string: *
    // GET	/patientdatasets/{pdsID}	For input string: *
    // GET	/patientdatasets/{pdsID}	XUUID type mismatch:  expected [O] but identifier has type *
    @Test
    public void getPatientDataSetWithInvalidID()
    {
        String invalidPDSID = "invalid";

        supressLogger();

        Response response = target()
                .path(PATIENTDATASETS).path("{pdsID}")
                .resolveTemplate("pdsID", invalidPDSID)
                .request()
                .get();

        Assert.assertEquals(response.getStatus(), 400);
    }

    // POST	/patientdatasets/{pdsID}/activate	Invalid UUID string: *
    @Test
    public void activatePatientDataSetWithInvalidID()
    {
        String invalidPDSID = "invalid";

        supressLogger();
        Map<String,String> params = new HashMap<>();

        // payload data = {"primary_assign_authority": aauth}
        params.put("primary_assign_authority", "PATIENT_ID_1");

        Response response = target()
                .path(PATIENTDATASETS).path("{pdsID}").path("activate")
                .resolveTemplate("pdsID", invalidPDSID)
                .request()
                .post(Entity.json(params));

        Assert.assertEquals(response.getStatus(), 400);
    }

    // POST	/patientdatasets/{pdsID}/deactivate	Invalid UUID string: *
    @Test
    public void deactivatePatientDataSetWithInvalidID()
    {
        String invalidPDSID = "invalid";

        supressLogger();
        Map<String,String> params = new HashMap<>();

        params.put("primary_assign_authority", "PATIENT_ID_1");

        Response response = target()
                .path(PATIENTDATASETS).path("{pdsID}").path("deactivate")
                .resolveTemplate("pdsID", invalidPDSID)
                .request()
                .post(Entity.json(params));

        Assert.assertEquals(response.getStatus(), 400);
    }

    // DELETE	/patientdatasets/{pdsID}/properties/{name}	XUUID type mismatch:  expected [O] but identifier has type *
    @Test
    public void deletePatientDataSetPropertyInvalidPdsID()
    {
        String invalidPDSID = "invalid";
        String propsName = "primary_assign_authority";
        supressLogger();

        Response response = target()
                .path(PATIENTDATASETS).path("{pdsID}").path("properties").path("{name}")
                .resolveTemplate("pdsID", invalidPDSID)
                .resolveTemplate("name", propsName)
                .request()
                .delete();

        Assert.assertEquals(response.getStatus(), 400);
    }
}
