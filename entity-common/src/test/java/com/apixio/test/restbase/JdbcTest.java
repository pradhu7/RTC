package com.apixio.test.restbase;

import java.io.File;

import com.apixio.restbase.config.ConfigSet;
import com.apixio.restbase.PersistenceServices;
import com.apixio.datasource.springjdbc.JdbcDS;
import com.apixio.datasource.springjdbc.EntitiesDS;

public class JdbcTest
{

    public static void main(String... args) throws Exception
    {
        ConfigSet           root = ConfigSet.fromYamlFile(new File(args[0]));
        PersistenceServices pst  = new PersistenceServices(root, null);
        JdbcDS              jdbc = pst.getJdbc("hi");

        (new EntitiesDS<Integer>(jdbc, null)).getValue("select count(*) from siggens", null, Integer.class);

        Thread.sleep(10000);
    }

}
