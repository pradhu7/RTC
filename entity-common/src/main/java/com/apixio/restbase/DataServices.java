package com.apixio.restbase;

import com.apixio.restbase.config.ConfigSet;
import com.apixio.restbase.dao.CustomProperties;
import com.apixio.restbase.dao.DataVersions;
import com.apixio.useracct.buslog.BatchLogic;
import com.apixio.useracct.buslog.PatientDataSetLogic;
import com.apixio.useracct.dao.PatientDataSets;
import com.apixio.useracct.dao.Users;

import java.util.ArrayList;
import java.util.List;

/**
 * This level introduces a "postInit" mechanism that allows
 * LogicBase-extended classes to get informed when all services within the
 * larger "sys services" object have been created.  This is done at this level
 * and not at PersistenceServices level as there are no LogicBase classes
 * for the Redis and Cassandra persistence services.
 *
 * DataServices are meant to be used in both microservice code and in non-service
 * code (e.g., hadoop jobs).
 */
public class DataServices extends PersistenceServices
{
    private Users  users;

    protected DataVersions      dataVersions;
    private CustomProperties    customProperties;
    private PatientDataSets     patientDatasets;
    private PatientDataSetLogic patientDatasetLogic;
    private BatchLogic          batchLogic;

    /**
     * Internal support in a paltry attempt to make a slight improvement on
     * bootup dependency problems.  Allows all BaseLogic-extended classes to
     * be initialized after construction.
     */
    private List<LogicBase> postInits = new ArrayList<>();  // stupid way to allow two-pass initialization ('cause we lack IoC)

    /**
     * Create new DataServices instance by using base persistence info from the seed; the passed-in
     * ConfigSet should be the config tree rooted at the top of business logic config.  Each bizlogic
     * component that needs config should have its own subkey off the root.  Additionally, to maintain
     * backwards compatibility with configuration and codebases, business logic should have reasonable
     * defaults for missing configuration.
     */
    public DataServices(DaoBase seed)
    {
        this(seed, null);
    }

    public DataServices(DaoBase seed, ConfigSet bizLogicConfig)
    {
        super(seed);

        this.dataVersions        = new DataVersions(seed);
        this.users               = new Users(seed);
        this.patientDatasets     = new PatientDataSets(seed, dataVersions);
        this.customProperties    = new CustomProperties(seed, dataVersions);
        this.patientDatasetLogic = addPostInit(new PatientDataSetLogic(this));
        this.batchLogic          = addPostInit(new BatchLogic(this, bizLogicConfig));  // needs to run after PatientDataSetLogic postInit (while in prototype)

        // don't call doPostInit here:  the topmost creator must call it
    }

    /**
     * Record the BaseLogic-extended class that needs post initialization
     */
    protected <T extends LogicBase> T addPostInit(T logic)
    {
        postInits.add(logic);

        return logic;
    }

    public void doPostInit()
    {
        for (LogicBase logic : postInits)
            logic.postInit();
    }

    /**
     * Getters
     */
    public BatchLogic          getBatchLogic()          { return batchLogic;          }
    public DataVersions        getDataVersions()        { return dataVersions;        }
    public PatientDataSetLogic getPatientDataSetLogic() { return patientDatasetLogic; }
    public PatientDataSets     getPatientDataSets()     { return patientDatasets;     }
    public Users               getUsers()               { return users;               }
    public CustomProperties    getCustomProperties()    { return customProperties;    }

    protected void setPatientDataSetLogic(PatientDataSetLogic logic)
    {
        if (logic != null)
            this.patientDatasetLogic = logic;
    }

}
