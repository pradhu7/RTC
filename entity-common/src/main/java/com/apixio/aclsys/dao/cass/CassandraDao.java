package com.apixio.aclsys.dao.cass;

import com.apixio.datasource.cassandra.CqlCrud;
import com.apixio.restbase.DaoBase;

/**
 * Base class for DAOs that talk to Cassadra.
 */
public class CassandraDao
{
    /**
     * To talk with Cassandra
     */
    protected CqlCrud     cqlCrud;
    protected String      cfName;    // name of column family that stores ACL info

    /**
     * Validate that we have a good CqlCrud and column family name.
     */
    protected CassandraDao(DaoBase base, String cfName)
    {
        if ((cfName == null) || (cfName.length() == 0))
            throw new IllegalArgumentException("Empty/null column family name supplied for ACL system");
        else if (base == null)
            throw new IllegalArgumentException("Null DaoBase!");

        this.cqlCrud = base.getPersistenceServices().getCqlCrud();
        this.cfName  = cfName;
    }

}
