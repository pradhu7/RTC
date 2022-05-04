package com.apixio.dao.customerproperties;

import com.apixio.dao.DAOTestUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by lschneider on 3/10/15.
 */
@Ignore("Integration")
public class CustomerPropertiesTest {

    private DAOTestUtils       util;
    private CustomerProperties customerProperties;

    @Before
    public void setUp() throws Exception
    {
        util = new DAOTestUtils();
        customerProperties = util.daoServices.getCustomerProperties();
    }

    @Test
    public void testSetProperty() {
        try {
            customerProperties.setAFSFolder("folderName", "1");
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }
}
