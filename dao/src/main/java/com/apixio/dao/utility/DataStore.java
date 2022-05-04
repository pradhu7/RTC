package com.apixio.dao.utility;

import java.util.HashMap;
import java.util.Map;

import com.apixio.restbase.config.ConfigSet;

/**
 * Base DAO for persistence.  Only behavior common across control and signal data
 * can go here
 */
public interface DataStore
{
    /**
     * Initialization of a set of datastores requires that one datastore be
     * able to get a reference to another datastore.  This is handled by doing
     * a two-phase initialization, with the first phase prompting the (already
     * created) DataStore instances to do creation actitivies, and the second
     * phase prompting them to link to another (if needed).
     */
    public enum InitPhase { CREATE, LINK }

    public static class InitContext
    {
        private Map<String, DataStore> dataStores = new HashMap<>();

        public DaoServices daoServices;
        public InitPhase   phase;
        public ConfigSet   config;

        public InitContext(DaoServices daoServices)
        {
            this(daoServices, null);
        }

        public InitContext(DaoServices daoServices, ConfigSet config)
        {
            this.daoServices = daoServices;
            this.config      = config;
        }

        public void initializeAll(DataStore... dss)
        {
            this.phase = InitPhase.CREATE;

            for (DataStore ds : dss)
            {
                ds.initialize(this);
                dataStores.put(ds.getDataStoreID(), ds);
            }

            this.phase = InitPhase.LINK;
            for (DataStore ds : dss)
                ds.initialize(this);
        }

        /**
         * This allows datastore1 to get a reference to datastore2.  The actual
         * method signature is generic only to avoid casting by client code.
         */
        public <T> T getDataStore(String id, Class<T> clz)
        {
            return (T) dataStores.get(id);
        }
    }

    /**
     *
     */
    public String getDataStoreID();

    public void initialize(InitContext context);

    default public void shutdown() { };

}
