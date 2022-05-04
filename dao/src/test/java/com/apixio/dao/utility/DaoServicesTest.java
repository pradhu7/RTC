package com.apixio.dao.utility;


import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.apixio.restbase.config.ConfigSet;
import com.apixio.dao.DAOTestUtils;

@Ignore("Integration")
public class DaoServicesTest {
    private DAOTestUtils util;
    private ConfigSet config;
    private DaoServices daoServices;

    @Before
    public void setUp() throws Exception {
        util = new DAOTestUtils();
        config = util.config;
        daoServices = util.daoServices;
    }

    @Test
    public void testsDaoServices() {
        System.out.println(config.toString());

        assertNotNull(daoServices.getCustomerProperties());
        assertNotNull(daoServices.getPropertyHelper());
        assertNotNull(daoServices.getS3Ops());
        assertNotNull(daoServices.getApixioStorage());
        assertNotNull(daoServices.getBlobDAO());
        // assertNotNull(daoServices.getCqlCache());
        assertNotNull(daoServices.getSeqStoreDAO());
        assertNotNull(daoServices.getIndexSearchDAO2());
        assertNotNull(daoServices.getPatientDAO2());
        assertNotNull(daoServices.getDocumentTraceDAO1());

        System.out.println("DONE");
    }

}

