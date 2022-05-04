package com.apixio.dao.utility;

import com.apixio.dao.utility.sql.SqlDaoUtility;
import com.apixio.dao.utility.sql.SqlOrdering;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SqlDaoUtilityTest {

    @Test
    public void testMakeSql() {
        Map<String, String> macros = new HashMap<>();
        macros.put("TEST_1", "goodbye");
        macros.put("TEST_2", "hello");

        String tpl = "you say {TEST_1} i say {TEST_2}";
        String sql = SqlDaoUtility.makeSql(macros, tpl);
        Assert.assertEquals(sql, "you say goodbye i say hello");
    }

    @Test
    public void testStrList() {
        List<String> ids = Arrays.asList("value1", "value2");
        String listStr = SqlDaoUtility.strList(ids);
        Assert.assertEquals(listStr, "'value1','value2'");
    }

    @Test
    public void testOrderBy() {
        SqlOrdering asc = new SqlOrdering("field1", true);
        SqlOrdering desc = new SqlOrdering("field2", false);
        String ordering = SqlDaoUtility.orderBy(Arrays.asList(asc, desc));
        Assert.assertEquals(ordering, "ORDER BY field1 ASC, field2 DESC");
    }

}
