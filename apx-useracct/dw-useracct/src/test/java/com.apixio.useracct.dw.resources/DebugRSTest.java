package com.apixio.useracct.dw.resources;

import com.apixio.XUUID;
import com.apixio.aclsys.buslog.AclLogic;
import com.apixio.datasource.redis.RedisOps;
import com.apixio.restbase.DaoBase;
import com.apixio.restbase.PersistenceServices;
import com.apixio.restbase.dao.DataVersions;
import com.apixio.useracct.PrivSysServices;
import com.apixio.useracct.buslog.RoleLogic;
import com.apixio.useracct.dao.Organizations;
import com.apixio.useracct.dw.ServiceConfiguration;
import com.apixio.useracct.entity.Organization;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.*;
import org.mockito.Mockito;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import java.io.IOException;

public class DebugRSTest extends JerseyTest
{

    private PrivSysServices sysServices;

    private static final String permissions = "some permissions";
    private static final XUUID xuuid = XUUID.create("test");

    @Override
    protected Application configure() {
        ServiceConfiguration configuration = Mockito.mock(ServiceConfiguration.class);
        sysServices = Mockito.mock(PrivSysServices.class);

        Mockito.when(sysServices.getAclLogic()).thenReturn(Mockito.mock(AclLogic.class));

        try {
            Mockito.when(sysServices.getAclLogic().dumpAclPermissions()).thenReturn(permissions);
        } catch (IOException e) {
            e.printStackTrace();
        }

        PersistenceServices persistenceServices = Mockito.mock(PersistenceServices.class);
        Mockito.when(persistenceServices.getRedisKeyPrefix()).thenReturn("test");
        Mockito.when(persistenceServices.getRedisOps()).thenReturn(Mockito.mock(RedisOps.class));

        DaoBase daoBase = new DaoBase(persistenceServices);

        DataVersions dataVersions = new DataVersions(daoBase);

        Organization organization = Mockito.mock(Organization.class);
        RoleLogic roleLogic = Mockito.mock(RoleLogic.class);

        Mockito.when(organization.getID()).thenReturn(xuuid);

        Organizations organizations = new Organizations(daoBase, dataVersions);

        Mockito.when(sysServices.getOrganizations()).thenReturn(organizations);

        Mockito.when(sysServices.getRoleLogic()).thenReturn(roleLogic);
        try {
            Mockito.when(sysServices.getRoleLogic().dumpOrgPermissions(organization.getID()))
                    .thenReturn(xuuid.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new ResourceConfig().register(new DebugRS(configuration, sysServices));
    }

    // given endpoint /debug/acl/orgs
    // when valid request params
    // then response http code 200
    @Test
    public void dumpOrgAcls() {
        //we cannot fully test current method because Organizations class is final

        Response response = target()
                .path("/debug/acl/orgs")
                .request()
                .get();

        String body = response.readEntity(String.class);

        Assert.assertEquals(response.getStatus(), 200);
        Assert.assertTrue(body.isEmpty());

        Mockito.verify(sysServices).getOrganizations();
        Mockito.verify(sysServices).getRoleLogic();
    }

    // given endpoint /debug/acl/lowlevel
    // when valid request params
    // then response http code 200
    @Test
    public void dumpLowLevelAcls() throws IOException {

        Response response = target()
                .path("/debug/acl/lowlevel")
                .request()
                .get();

        String body = response.readEntity(String.class);

        Assert.assertEquals(response.getStatus(), 200);
        Assert.assertEquals(body, permissions);

        Mockito.verify(sysServices, Mockito.times(2)).getAclLogic();
        Mockito.verify(sysServices.getAclLogic(), Mockito.times(1)).dumpAclPermissions();
    }
}