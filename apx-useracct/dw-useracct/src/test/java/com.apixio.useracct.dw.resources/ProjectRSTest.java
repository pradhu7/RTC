package com.apixio.useracct.dw.resources;

import com.apixio.XUUID;
import com.apixio.restbase.DaoBase;
import com.apixio.restbase.PropertyUtil;
import com.apixio.restbase.RestUtil;
import com.apixio.restbase.dao.CachingBase;
import com.apixio.restbase.dao.DataVersions;
import com.apixio.restbase.dao.EntitySetBase;
import com.apixio.restbase.entity.Token;
import com.apixio.restbase.web.BaseRS;
import com.apixio.useracct.PrivSysServices;
import com.apixio.useracct.buslog.HccProjectDTO;
import com.apixio.useracct.buslog.OrganizationLogic;
import com.apixio.useracct.buslog.ProjectLogic;
import com.apixio.useracct.buslog.ProjectProperties;
import com.apixio.useracct.dao.Projects;
import com.apixio.useracct.dw.ServiceConfiguration;
import com.apixio.useracct.entity.*;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.glassfish.jersey.test.DeploymentContext;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.ServletDeploymentContext;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.api.support.membermodification.MemberMatcher;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.*;

import static org.mockito.Matchers.any;

@PowerMockIgnore("javax.management.*")
@RunWith(PowerMockRunner.class)
@PrepareForTest({ProjectRS.class, BaseRS.class,
        ProjectLogic.class, DaoBase.class,
        DataVersions.class, Projects.class,
        RestUtil.class, CachingBase.class,
        EntitySetBase.class, ProjectProperties.class,
        Project.class, Organization.class,
        PatientDataSet.class})
public class ProjectRSTest extends JerseyTest
{

    private static final String PROJECTS = "projects";
    private static final String MEMBERS = "members";
    public static final String PROPDEFS = "propdefs";
    private PrivSysServices sysServices;
    private ProjectRS projectRS;

    private ProjectRS.ProjParams projParams;
    private XUUID projectID1 = XUUID.fromString("PR_dddddddd-1195-42ad-99d0-3ec12ab51dc6");
    private XUUID organizationID1 = XUUID.fromString("UO_dddddddd-1195-42ad-99d0-3ec12ab51dc6");
    private XUUID patientDataSetID1 = XUUID.fromString("O_dddddddd-1195-42ad-99d0-3ec12ab51dc6");

    public void initTestData()
    {
        projParams = new ProjectRS.ProjParams();
        projParams.organizationID = organizationID1.getID();
        projParams.createdAt = "2019-01-01T10:30:00Z";
        projParams.name = projectID1.getID();
        projParams.description = "description";
        projParams.type = HccProject.Type.TEST.name();
        projParams.organizationID = organizationID1.getID();
        projParams.patientDataSetID = patientDataSetID1.getID();
        projParams.dosStart = "2019-01-02T10:30:00Z";
        projParams.dosEnd = "2019-01-03T10:30:00Z";
        projParams.paymentYear = "2019";
        projParams.sweep = "midYear";
        projParams.passType = "First Pass";
        projParams.status = false;
        projParams.state = "new";
        projParams.rawRaf = 0.0;
        projParams.raf = 0.0;
        projParams.budget = 10.0;
        projParams.deadline = "2019-01-02T10:30:00Z";
        projParams.datasource = "some_data_source";
        projParams.patientList = "some_patient_list";
        projParams.docFilterList = "some_doc_filter_list";
    }

    @Override
    protected TestContainerFactory getTestContainerFactory() {
        return new GrizzlyWebTestContainerFactory();
    }

    // Here are configuring a servlet for request and response
    // ProjectRS service is just partially mocked by PowerMockito
    @Override
    protected DeploymentContext configureDeployment() {
        sysServices = PowerMockito.mock(PrivSysServices.class);
        ServiceConfiguration configuration = PowerMockito.mock(ServiceConfiguration.class);
        projectRS = PowerMockito.spy(new ProjectRS(configuration, sysServices));

        return ServletDeploymentContext.forServlet(
                new ServletContainer(new ResourceConfig().register(projectRS))).build();
    }

    private void createProjectPostGenericMock() throws Exception
    {
        PowerMockito.when(sysServices.getProjectLogic()).thenReturn(PowerMockito.mock(ProjectLogic.class));
        PowerMockito.when(sysServices.getProjectLogic().createHccProject(any(HccProjectDTO.class)))
                .thenReturn(Mockito.mock(HccProject.class));
        PowerMockito.when(sysServices.getProjectLogic().createHccProject(any(HccProjectDTO.class)).getID())
                .thenReturn(projectID1);

        supressLogger();

        PowerMockito.doNothing().when(projectRS, "logParameters",
                any(BaseRS.ApiLogger.class), any(XUUID.class), any(ProjectRS.ProjParams.class));
    }

    private void supressLogger() {
        PowerMockito.suppress(PowerMockito.method(BaseRS.ApiLogger.class, "addParameter", String.class, Object.class));
    }


    //POST:/projects
    //    body:  {"organizationID":"{xuuid}", "patientDataSetID":"{}", "name":"{}", "description":"{}", "type":"{}"}
    //    returns:  {"id":"..."}
    @Test
    public void createProjectPostPositive() throws Exception
    {
        initTestData();
        createProjectPostGenericMock();

        Response response = target()
                .path(PROJECTS)
                .request()
                .post(Entity.json(projParams));

        String body = response.readEntity(String.class);

        Assert.assertEquals(response.getStatus(), 200);
        Assert.assertEquals(body, "{\"id\":\"PR_dddddddd-1195-42ad-99d0-3ec12ab51dc6\"}");

        PowerMockito.verifyPrivate(projectRS).invoke("logParameters",
                any(BaseRS.ApiLogger.class), any(XUUID.class), any(ProjectRS.ProjParams.class));

        Mockito.verify(sysServices, Mockito.times(3)).getProjectLogic();
    }

    @Test
    public void createProjectPostWithInvalidXUUID() throws Exception
    {
        initTestData();
        createProjectPostGenericMock();
        projParams.organizationID = "invalid_id";

        Response response = target()
                .path(PROJECTS)
                .request()
                .post(Entity.json(projParams));

        Assert.assertEquals(response.getStatus(), 400);
    }

    //POST	/projects	Name of NamedEntity can't be null
    @Test
    public void createProjectPostWithEmptyEntity() throws Exception
    {
        createProjectPostGenericMock();

        Response response = target()
                .path(PROJECTS)
                .request()
                .post(Entity.json(""));

        Assert.assertEquals(response.getStatus(), 400);
    }

    //POST	/projects	Name of NamedEntity can't be empty
    @Test
    public void createProjectPostWithNullEntity() throws Exception
    {
        createProjectPostGenericMock();

        Response response = target()
                .path(PROJECTS)
                .request()
                .post(Entity.json(null));

        Assert.assertEquals(response.getStatus(), 400);
    }

    @Test
    public void createProjectPostSomeServerError() throws Exception
    {
        initTestData();
        supressLogger();

        PowerMockito.when(sysServices.getProjectLogic()).thenReturn(PowerMockito.mock(ProjectLogic.class));
        PowerMockito.when(sysServices.getProjectLogic()
                .createHccProject(any(HccProjectDTO.class))).thenThrow(Exception.class);

        Response response = target()
                .path(PROJECTS)
                .request()
                .post(Entity.json(projParams));

        Assert.assertEquals(response.getStatus(), 500);
    }

    //GET	/projects/{projID}	Invalid UUID string: *
    @Test
    public void getProjectsWithNotExistingOrUnexpectedProjectId() throws Exception
    {

        supressLogger();

        String projectXuuid = "invalid_id";

        partiallyMockingProjectLogic();

        Response response = target()
                .path(PROJECTS).path(projectXuuid)
                .request()
                .get();

        Assert.assertEquals(response.getStatus(), 400);
    }

    private Projects partiallyMockingProjectLogic() throws IllegalAccessException
    {
        PowerMockito.suppress(PowerMockito.constructor(ProjectLogic.class));
        ProjectLogic projectLogic = new ProjectLogic(sysServices);

        PowerMockito.suppress(PowerMockito.constructor(Projects.class));
        Projects projects = PowerMockito.spy(new Projects(PowerMockito.mock(DaoBase.class), PowerMockito.mock(DataVersions.class)));

        MemberMatcher.field(ProjectLogic.class, "projects").set(projectLogic, projects);
        PowerMockito.when(sysServices.getProjectLogic()).thenReturn(projectLogic);

        return projects;
    }

    //GET	/projects/{projID}	Invalid ProjectID Type
    @Test
    public void getProjectsWithInvalidProjectIdType() throws Exception
    {

        String projectXuuid = "invalid_id";

        supressLogger();
        partiallyMockingProjectLogic();

        Response response = target()
                .path(PROJECTS).path(projectXuuid)
                .request()
                .get();

        Assert.assertEquals(response.getStatus(), 400);
    }

    // GET /projects orgID Invalid Type
    // XUUID type mismatch:  expected [UO] but identifier has type *
    @Test
    public void getProjectsWithInValidOrgIDType() throws Exception
    {

        // Invalid Org Type
        String projectXuuid = "U_e98afdea-24de-4ad0-92c9-10aa4267aac7";

        supressLogger();
        partiallyMockingProjectLogic();
        mockRestUtil();

        Response response = target()
                .path(PROJECTS)
                .queryParam("orgID", projectXuuid)
                .request()
                .get();

        Assert.assertEquals(response.getStatus(), 400);
    }

    //  GET /projects/{projID}/members For input string: *
    @Test
    public void getProjectMembersWithInvalidProjectId() throws Exception
    {

        String projectXuuid = "invalid_id";

        supressLogger();
        partiallyMockingProjectLogic();

        Response response = target()
                .path(PROJECTS).path(projectXuuid).path(MEMBERS)
                .request()
                .get();

        Assert.assertEquals(response.getStatus(), 400);
    }

    //  GET /projects/{projID}/members Invalid UUID string: *
    @Test
    public void getProjectMembersWithPartiallyInvalidProjectId() throws Exception
    {

        String projectXuuid = "XX_e98afdea-24de-4ad0-92c9-10aa4267aac7";

        supressLogger();
        Projects projects = partiallyMockingProjectLogic();
        MemberMatcher.field(EntitySetBase.class, "allowedObjTypes").set(projects, Arrays.asList("PR*", "CP"));

        Response response = target()
                .path(PROJECTS).path(projectXuuid).path(MEMBERS)
                .request()
                .get();

        Assert.assertEquals(response.getStatus(), 400);
    }

    //  GET /projects/{projID}/members Invalid UUID Type
    @Test
    public void getProjectMembersWithProjectIdPartiallyValidType() throws Exception
    {

        String projectXuuid = "PRQUAL_e98afdea-24de-4ad0-92c9-10aa4267aac7";

        supressLogger();
        ProjectLogic projectLogic = PowerMockito.mock(ProjectLogic.class);
        PowerMockito.when(sysServices.getProjectLogic()).thenReturn(projectLogic);
        PowerMockito.when(projectLogic.getProjectByID(any(String.class))).thenReturn(null);

        Response response = target()
                .path(PROJECTS).path(projectXuuid).path(MEMBERS)
                .request()
                .get();

        Assert.assertEquals(response.getStatus(), 404);
    }

    //  #GET /projects Unknown property set name *
    @Test
    public void getProjectWithUnknownProperty() throws Exception
    {

        String propertyString = "invalid";

        supressLogger();
        mockRestUtil();

        ProjectLogic projectLogic = PowerMockito.mock(ProjectLogic.class);
        PowerMockito.when(sysServices.getProjectLogic()).thenReturn(projectLogic);

        Project proj = PowerMockito.mock(Project.class);
        List<Project> projs = new ArrayList<>();
        projs.add(proj);

        PowerMockito.when(projectLogic, "getProjects", any(XUUID.class),
                any(String.class), any(String.class),
                any(String.class), any(boolean.class)).thenReturn(projs);

        PowerMockito.mockStatic(ProjectProperties.class);
        PowerMockito.doReturn(PowerMockito.mock(Organization.class)).when(projectRS, "getOrganization", any(Project.class));
        PowerMockito.doReturn(PowerMockito.mock(PatientDataSet.class)).when(projectRS, "getPds", any(Project.class));
        PowerMockito.when(ProjectProperties.toMap(any(Project.class), any(Organization.class),
                any(PatientDataSet.class))).thenReturn(new HashMap<String, Object>()) ;

        Response response = target()
                .path(PROJECTS).queryParam("properties", propertyString)
                .request()
                .get();

        Assert.assertEquals(response.getStatus(), 400);
    }

    private void mockRestUtil()
    {
        PowerMockito.mockStatic(RestUtil.class);
        PowerMockito.when(RestUtil.getInternalToken()).thenReturn(PowerMockito.mock(Token.class));
    }

    // GET	/projects/properties/{bag}	Unknown property set name *
    @Test
    public void getProjectsPropertyWithUnknownPropertyBag() throws Exception
    {

        String bagString = "invalid";

        supressLogger();

        Response response = target()
                .path(PROJECTS).path("properties").path("{bag}")
                .resolveTemplate("bag",bagString)
                .request()
                .get();

        Assert.assertEquals(response.getStatus(), 400);
    }

    // PUT	/projects/{projID}/properties/{bag}/{name}	For input string: *
    @Test
    @Ignore
    public void putPropertyForInvalidProject() throws Exception
    {
        String projUUID = "invalid";
        Map<String, String> jsonobj = new HashMap<>();
        jsonobj.put("value", "something");

        supressLogger();
        partiallyMockingProjectLogic();

        Response response = target()
                .path(PROJECTS).path("{projID}").path("properties").path("{bag}").path("name")
                .resolveTemplate("projID", projUUID)
                .resolveTemplate("bag","gen")
                .request()
                .put(Entity.json(jsonobj));

        Assert.assertEquals(response.getStatus(), 400);
    }

    // DELETE	/projects/{projID}/properties/{bag}/{name}	Unknown property set name *
    @Test
    public void deletePropertyFromProjectWithInvalidPropertyBag() throws Exception
    {
        String invalidBagName = "invalid";

        supressLogger();
        PowerMockito.doReturn(PowerMockito.mock(Project.class)).when(projectRS, "checkedGetProject", any(String.class));

        Response response = target()
                .path(PROJECTS).path("{projID}").path("properties").path("{bag}").path("name")
                .resolveTemplate("projID", projectID1)
                .resolveTemplate("bag",invalidBagName)
                .request()
                .delete();

        Assert.assertEquals(response.getStatus(), 400);
    }

    // PUT	/projects/{projID}/users/{userID}/roles/{role}	Attempt to find object using the wrong type of XUUID; *
    @Test
    public void putProjectRoleWithInvalidProjIDType() throws IllegalAccessException
    {
        // valid Project ID should start with PR* or CP
        String invalidProjIdType = "U_792ced3b-6ca1-4348-9db4-495c933ecde4";
        String roleID = "R2_92d51208-ee14-44c2-8f2a-a85958c2b15d";
        String userID = "U_792ced3b-6ca1-4348-9db4-495c933ecde4";

        supressLogger();
        Projects projects = partiallyMockingProjectLogic();
        MemberMatcher.field(EntitySetBase.class, "allowedObjTypes").set(projects, Arrays.asList("PR*", "CP"));

        Response response = target()
                .path(PROJECTS).path("{projID}").path("users").path("{userID}").path("roles").path("{role}")
                .resolveTemplate("projID", invalidProjIdType)
                .resolveTemplate("userID", userID)
                .resolveTemplate("role", roleID)
                .request()
                .put(Entity.json(new HashMap()));

        Assert.assertEquals(response.getStatus(), 400);
    }

    // POST	/projects/propdefs/{bag}	No enum constant com.apixio.restbase.PropertyType.*
    @Test
    public void postPropertyDefsWithInvalidType() throws Exception
    {
        String propertytype = "invalid";
        String property = "newprop";
        String bag = "gen";
        Map<String, String> params = new HashMap<>();
        params.put("name", property);
        params.put("type", propertytype);

        supressLogger();
        PowerMockito.suppress((PowerMockito.constructor(ProjectLogic.class)));
        ProjectLogic projectLogic = PowerMockito.spy(new ProjectLogic(sysServices));
        PropertyUtil propertyUtil = PowerMockito.spy(new PropertyUtil("Project.generic", sysServices));
        MemberMatcher.field(ProjectLogic.class, "genericPropertyUtil").set(projectLogic, propertyUtil);
        PowerMockito.when(sysServices.getProjectLogic()).thenReturn(projectLogic);

        Response response = target()
                .path(PROJECTS).path(PROPDEFS).path("{bag}")
                .resolveTemplate("bag", bag)
                .request()
                .post(Entity.json(params));

        Assert.assertEquals(response.getStatus(), 400);
    }

    // DELETE	/projects/propdefs/{bag}/{name}	Unknown property set name *
    @Test
    public void deleteProjectPropertyWithInvalidBagName()
    {
        String bagName = "invalid";

        supressLogger();

        Response response = target()
                .path(PROJECTS).path(PROPDEFS).path("{bag}").path("{name}")
                .resolveTemplate("bag", bagName)
                .resolveTemplate("name", "something")
                .request()
                .delete();

        Assert.assertEquals(response.getStatus(), 400);
    }

    // PUT /project/{projid} fails because of incomplete datetime format
    @Test
    @Ignore
    public void updateProjectDatePropertyWithInvalidDate() throws Exception
    {
        String dateString = "2019-01-01";
        String projectID = "PRHCC_ffed01dc-c7c2-4191-a70d-eae9894a4bc8";
        Map<String,String> parameters = new HashMap<>();
        parameters.put("deadline", dateString);

        supressLogger();

        ProjectLogic projectLogic = PowerMockito.mock(ProjectLogic.class);
        Project project = PowerMockito.mock(HccProject.class);

        PowerMockito.when(sysServices.getProjectLogic()).thenReturn(projectLogic);
        PowerMockito.when(projectLogic.getProjectByID(any(String.class))).thenReturn(project);
        PowerMockito.when(project.getProjectClass()).thenReturn(Project.ProjectClass.HCC);

        Response response = target()
                .path(PROJECTS).path("{projID}")
                .resolveTemplate("projID", projectID)
                .request()
                .put(Entity.json(parameters));

        Assert.assertEquals(response.getStatus(), 400);

    }

    @Test
    public void givenVendorSupervisorRole_whenFilterProjectUsersByUserRole_thenReturnFilteredList() throws IOException {
        //// Setup mock dependencies and setup test fixture
        ProjectLogic projectLogicMock = Mockito.mock(ProjectLogic.class);
        OrganizationLogic orgLogicMock = Mockito.mock(OrganizationLogic.class);
        User userMock = PowerMockito.mock(User.class);

        String user1Id = "U_084c9f97-22fb-4ef9-baa3-3c7738b738a6";
        String user2Id = "U_084c9f97-22fb-4ef9-baa3-3c7738b738a7";
        String projectId = "PRHCC_c09b67db-8e99-43f4-b742-74aec94101d0";
        List<UserProject> userProjects = Arrays.asList(
                new UserProject(XUUID.fromString(user1Id), XUUID.fromString(projectId)),
                new UserProject(XUUID.fromString(user2Id), XUUID.fromString(projectId))
        );

        String user1OrgId = "UO_e98afdea-24de-4ad0-92c9-10aa4267aac7";
        String user2OrgId = "UO_e98afdea-24de-4ad0-92c9-10aa4267aac8";
        Organization user1OrgMock = Mockito.mock(Organization.class);
        Mockito.when(user1OrgMock.getID()).thenReturn(XUUID.fromString(user1OrgId));
        Organization user2OrgMock = Mockito.mock(Organization.class);
        Mockito.when(user2OrgMock.getID()).thenReturn(XUUID.fromString(user2OrgId));
        Mockito.when(orgLogicMock.getUsersOrganizations(XUUID.fromString(user1Id))).thenReturn(Arrays.asList(user1OrgMock));
        Mockito.when(orgLogicMock.getUsersOrganizations(XUUID.fromString(user2Id))).thenReturn(Arrays.asList(user2OrgMock));

        PowerMockito.when(projectLogicMock.getHccRolePermittedOrgs(any(), any())).thenReturn(Collections.singleton(user1OrgId));

        //// Test
        List<UserProject> result = projectRS.filterProjectUsersByUserRoleOrg(projectId, userProjects, projectLogicMock, orgLogicMock, userMock);

        //// Assert results
        Assert.assertEquals(result.size(), 1);
        Assert.assertEquals(user1Id ,result.get(0).getUserID().getID());
    }

    @Test
    public void givenCustOpsRole_whenFilterProjectUsersByUserRole_thenReturnFullList() throws IOException {
        //// Setup mock dependencies and setup test fixture
        ProjectLogic projectLogicMock = Mockito.mock(ProjectLogic.class);
        OrganizationLogic orgLogicMock = Mockito.mock(OrganizationLogic.class);
        User userMock = PowerMockito.mock(User.class);

        String user1Id = "U_084c9f97-22fb-4ef9-baa3-3c7738b738a6";
        String user2Id = "U_084c9f97-22fb-4ef9-baa3-3c7738b738a7";
        String projectId = "PRHCC_c09b67db-8e99-43f4-b742-74aec94101d0";
        List<UserProject> userProjects = Arrays.asList(
                new UserProject(XUUID.fromString(user1Id), XUUID.fromString("PRHCC_c09b67db-8e99-43f4-b742-74aec94101d0")),
                new UserProject(XUUID.fromString(user2Id), XUUID.fromString("PRHCC_c09b67db-8e99-43f4-b742-74aec94101d0"))
        );

        String user1OrgId = "UO_e98afdea-24de-4ad0-92c9-10aa4267aac7";
        String user2OrgId = "UO_e98afdea-24de-4ad0-92c9-10aa4267aac8";
        Organization user1OrgMock = Mockito.mock(Organization.class);
        Mockito.when(user1OrgMock.getID()).thenReturn(XUUID.fromString(user1OrgId));
        Organization user2OrgMock = Mockito.mock(Organization.class);
        Mockito.when(user2OrgMock.getID()).thenReturn(XUUID.fromString(user2OrgId));
        Mockito.when(orgLogicMock.getUsersOrganizations(XUUID.fromString(user1Id))).thenReturn(Arrays.asList(user1OrgMock));
        Mockito.when(orgLogicMock.getUsersOrganizations(XUUID.fromString(user2Id))).thenReturn(Arrays.asList(user2OrgMock));

        PowerMockito.when(projectLogicMock.getHccRolePermittedOrgs(any(), any())).thenReturn(Collections.emptySet());

        //// Test
        List<UserProject> result = projectRS.filterProjectUsersByUserRoleOrg(projectId, userProjects, projectLogicMock, orgLogicMock, userMock);

        //// Assert results
        Assert.assertEquals(result.size(), 2);
    }

}