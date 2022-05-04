package com.apixio.useracct.buslog;

import com.apixio.SysServices;
import com.apixio.XUUID;
import com.apixio.datasource.redis.DistLock;
import com.apixio.datasource.redis.RedisOps;
import com.apixio.restbase.DaoBase;
import com.apixio.useracct.dao.Projects;
import com.apixio.useracct.dao.Users;
import com.apixio.useracct.entity.Organization;
import com.apixio.useracct.entity.Role;
import com.apixio.useracct.entity.User;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import static org.powermock.api.mockito.PowerMockito.*;

import java.io.IOException;
import java.util.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ProjectLogic.class, SysServices.class, DistLock.class, Projects.class, DaoBase.class})
public class ProjectLogicHccRolesTest {
    private Projects projectsMock;
    private Users usersMock;
    private UserProjectLogic userProjectLogicMock;
    private RedisOps redisOpsMock;
    private OrganizationLogic orgLogicMock;
    private RoleLogic roleLogicMock;
    private PatientDataSetLogic patientDataSetLogicMock;

    private Organization organizationMock;
    private User userMock;

    private SysServices sysServicesMock;
    private ProjectLogic projectLogic;

    @Before
    public void setUp()
    {
        // Mock ProjectLogic dependencies
        projectsMock = mock(Projects.class);
        usersMock = mock(Users.class);
        userProjectLogicMock = mock(UserProjectLogic.class);
        redisOpsMock = mock(RedisOps.class);
        orgLogicMock = mock(OrganizationLogic.class);
        roleLogicMock = mock(RoleLogic.class);
        patientDataSetLogicMock = mock(PatientDataSetLogic.class);

        organizationMock = mock(Organization.class);
        userMock = mock(User.class);

        sysServicesMock = mock(SysServices.class);

        when(sysServicesMock.getProjects()).thenReturn(projectsMock);
        when(sysServicesMock.getUsers()).thenReturn(usersMock);
        when(sysServicesMock.getUserProjectLogic()).thenReturn(userProjectLogicMock);
        when(sysServicesMock.getRedisOps()).thenReturn(redisOpsMock);
        when(sysServicesMock.getOrganizationLogic()).thenReturn(orgLogicMock);
        when(sysServicesMock.getRoleLogic()).thenReturn(roleLogicMock);
        when(sysServicesMock.getPatientDataSetLogic()).thenReturn(patientDataSetLogicMock);
        when(sysServicesMock.getMicroServiceName()).thenReturn("MicroServiceName");

        // Test class
        projectLogic = new ProjectLogic(sysServicesMock);
        projectLogic.postInit();
    }

    @Test
    public void givenCustomerOpsRole_whenGetUserRolePermittedOrgsForProject_thenReturnAllOrgs() throws IOException {
        //// Setup mock dependencies and setup test fixture
        String userOrgId = "UO_e98afdea-24de-4ad0-92c9-10aa4267aac7";
        String roleSetId = "RS_a4f24115-0ddd-486d-8132-0c820ee59db8";
        String projectId = "PRHCC_60a727ef-5a31-44c0-b2d3-4b3431dc4a7b";
        String userId = "U_d9d4c914-ae09-4a8b-b7a5-a4b237339f48";
        List<RoleAssignment> roles = Collections.singletonList(
                new RoleAssignment(null, XUUID.fromString(userOrgId), new Role(XUUID.fromString(roleSetId), Role.CUSTOMEROPS))
        );
        when(roleLogicMock.getRolesAssignedToUser(Mockito.any())).thenReturn(roles);

        //// Test
        Set<String> result = projectLogic.getHccRolePermittedOrgs(userMock, projectId);

        //// Assert results
        Assert.assertEquals(Collections.singleton("*") ,result);
    }

    @Test
    public void givenSupervisorRole_whenGetUserRolePermittedOrgsForProject_thenReturnAllOrgs() throws IOException {
        //// Setup mock dependencies and setup test fixture
        String userOrgId = "UO_e98afdea-24de-4ad0-92c9-10aa4267aac7";
        String roleSetId = "RS_a4f24115-0ddd-486d-8132-0c820ee59db8";
        String projectId = "PRHCC_60a727ef-5a31-44c0-b2d3-4b3431dc4a7b";
        String userId = "U_d9d4c914-ae09-4a8b-b7a5-a4b237339f48";
        List<RoleAssignment> roles = Collections.singletonList(
                new RoleAssignment(null, XUUID.fromString(projectId), new Role(XUUID.fromString(roleSetId), Role.SUPERVISOR))
        );
        when(roleLogicMock.getRolesAssignedToUser(Mockito.any())).thenReturn(roles);

        //// Test
        Set<String> result = projectLogic.getHccRolePermittedOrgs(userMock, projectId);

        //// Assert results
        Assert.assertEquals(Collections.singleton("*") ,result);
    }

    @Test
    public void givenOrgSupervisorRole_whenGetUserRolePermittedOrgsForProject_thenReturnUserRoleTargetedOrgs() throws IOException {
        //// Setup mock dependencies and setup test fixture
        String userOrgId = "UO_e98afdea-24de-4ad0-92c9-10aa4267aac7";
        String roleSetId = "RS_a4f24115-0ddd-486d-8132-0c820ee59db8";
        String projectId = "PRHCC_60a727ef-5a31-44c0-b2d3-4b3431dc4a7b";
        String userId = "U_d9d4c914-ae09-4a8b-b7a5-a4b237339f48";
        List<RoleAssignment> roles = Collections.singletonList(
                new RoleAssignment(null, XUUID.fromString(userOrgId), new Role(XUUID.fromString(roleSetId), Role.ORG_SUPERVISOR))
        );
        when(roleLogicMock.getRolesAssignedToUser(Mockito.any())).thenReturn(roles);

        //// Test
        Set<String> result = projectLogic.getHccRolePermittedOrgs(userMock, projectId);

        //// Assert results
        Assert.assertEquals(Collections.singleton(userOrgId) ,result);
    }

    @Test
    public void givenVendorSupervisorRole_whenGetUserRolePermittedOrgsForProject_thenReturnUserOwnOrg() throws IOException {
        //// Setup mock dependencies and setup test fixture
        String userOrgId = "UO_e98afdea-24de-4ad0-92c9-10aa4267aac7";
        String roleSetId = "RS_a4f24115-0ddd-486d-8132-0c820ee59db8";
        String projectId = "PRHCC_60a727ef-5a31-44c0-b2d3-4b3431dc4a7b";
        String userId = "U_d9d4c914-ae09-4a8b-b7a5-a4b237339f48";
        List<RoleAssignment> roles = Collections.singletonList(
                new RoleAssignment(null, XUUID.fromString(projectId), new Role(XUUID.fromString(roleSetId), Role.VENDOR_SUPERVISOR))
        );
        when(roleLogicMock.getRolesAssignedToUser(Mockito.any())).thenReturn(roles);

        when(organizationMock.getID()).thenReturn(XUUID.fromString(userOrgId));
        List<Organization> orgList = Collections.singletonList(organizationMock);
        when(orgLogicMock.getUsersOrganizations(Mockito.any())).thenReturn(orgList);
        when(userMock.getID()).thenReturn(XUUID.fromString(userId));

        //// Test
        Set<String> result = projectLogic.getHccRolePermittedOrgs(userMock, projectId);

        //// Assert results
        Assert.assertEquals(Collections.singleton(userOrgId) ,result);
    }

    @Test
    public void givenClientSupervisorRole_whenGetUserRolePermittedOrgsForProject_thenReturnUserOwnOrg() throws IOException {
        //// Setup mock dependencies and setup test fixture
        String userOrgId = "UO_e98afdea-24de-4ad0-92c9-10aa4267aac7";
        String roleSetId = "RS_a4f24115-0ddd-486d-8132-0c820ee59db8";
        String projectId = "PRHCC_60a727ef-5a31-44c0-b2d3-4b3431dc4a7b";
        String userId = "U_d9d4c914-ae09-4a8b-b7a5-a4b237339f48";
        List<RoleAssignment> roles = Collections.singletonList(
                new RoleAssignment(null, XUUID.fromString(projectId), new Role(XUUID.fromString(roleSetId), Role.CLIENT_SUPERVISOR))
        );
        when(roleLogicMock.getRolesAssignedToUser(Mockito.any())).thenReturn(roles);

        when(organizationMock.getID()).thenReturn(XUUID.fromString(userOrgId));
        List<Organization> orgList = Collections.singletonList(organizationMock);
        when(orgLogicMock.getUsersOrganizations(Mockito.any())).thenReturn(orgList);
        when(userMock.getID()).thenReturn(XUUID.fromString(userId));

        //// Test
        Set<String> result = projectLogic.getHccRolePermittedOrgs(userMock, projectId);

        //// Assert results
        Assert.assertEquals(Collections.singleton(userOrgId) ,result);
    }

    @Test
    public void givenOtherRole_whenGetUserRolePermittedOrgsForProject_thenReturnEmptySet() throws IOException {
        //// Setup mock dependencies and setup test fixture
        String userOrgId = "UO_e98afdea-24de-4ad0-92c9-10aa4267aac7";
        String roleSetId = "RS_a4f24115-0ddd-486d-8132-0c820ee59db8";
        String projectId = "PRHCC_60a727ef-5a31-44c0-b2d3-4b3431dc4a7b";
        String userId = "U_d9d4c914-ae09-4a8b-b7a5-a4b237339f48";
        List<RoleAssignment> roles = Collections.singletonList(
                new RoleAssignment(null, XUUID.fromString(userOrgId), new Role(XUUID.fromString(roleSetId), Role.APIXIO_NON_PHI))
        );
        when(roleLogicMock.getRolesAssignedToUser(Mockito.any())).thenReturn(roles);

        //// Test
        Set<String> result = projectLogic.getHccRolePermittedOrgs(userMock, projectId);

        //// Assert results
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void givenReviewerRole_whenGetUserRolePermittedOrgsForProject_thenReturnUserOwnOrg() throws IOException {
        //// Setup mock dependencies and setup test fixture
        String userOrgId = "UO_e98afdea-24de-4ad0-92c9-10aa4267aac7";
        String roleSetId = "RS_a4f24115-0ddd-486d-8132-0c820ee59db8";
        String projectId = "PRHCC_60a727ef-5a31-44c0-b2d3-4b3431dc4a7b";
        String userId = "U_d9d4c914-ae09-4a8b-b7a5-a4b237339f48";
        List<RoleAssignment> roles = Collections.singletonList(
                new RoleAssignment(null, XUUID.fromString(projectId), new Role(XUUID.fromString(roleSetId), Role.REVIEWER))
        );
        when(roleLogicMock.getRolesAssignedToUser(Mockito.any())).thenReturn(roles);

        when(organizationMock.getID()).thenReturn(XUUID.fromString(userOrgId));
        List<Organization> orgList = Collections.singletonList(organizationMock);
        when(orgLogicMock.getUsersOrganizations(Mockito.any())).thenReturn(orgList);
        when(userMock.getID()).thenReturn(XUUID.fromString(userId));

        //// Test
        Set<String> result = projectLogic.getHccRolePermittedOrgs(userMock, projectId);

        //// Assert results
        Assert.assertEquals(Collections.singleton(userOrgId) ,result);
    }
}
