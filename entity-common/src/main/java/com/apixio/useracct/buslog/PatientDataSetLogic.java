package com.apixio.useracct.buslog;

import java.util.*;
import java.util.regex.Pattern;

import com.apixio.Datanames;
import com.apixio.datasource.cassandra.FieldValueList;
import com.apixio.datasource.cassandra.FieldValueListBuilder;
import com.apixio.datasource.cassandra.StatementGenerator;
import com.apixio.datasource.utility.LockUtility;
import com.apixio.restbase.DataServices;
import com.apixio.XUUID;
import com.apixio.datasource.cassandra.CqlCrud;
import com.apixio.logger.EventLogger;
import com.apixio.restbase.LogicBase;
import com.apixio.restbase.PersistenceServices;
import com.apixio.restbase.PropertyType;
import com.apixio.restbase.PropertyUtil;
import com.apixio.restbase.web.BaseException;
import com.apixio.useracct.dao.PatientDataSets;
import com.apixio.useracct.entity.PatientDataSet;
import com.apixio.utility.StringUtil;

/**
 * PatientDataSetLogic exposes business logic on PatientDataSet entities and their custom properties.
 *
 * Design note:  the design assumes that there is a need to "cut across" the set of patientDataSets
 * and rules (etc.) and that there are hundreds, if not thousands of such elements, and that
 * doing a simple one-by-one query against redis will be painfully slow.  Therefore, the design
 * is to cache all (yes, all!) of these structures in the JVM process.  While most of this
 * caching is done automatically and is hidden from this level, it does lead to some code that
 * might look un-optimal (such as requesting all objects and then looping through them to search
 * for a single object that matches some criteria).
 *
 * The lower-level design (at the DAO level, which really defines the redis-level structures)
 * takes advantage of this caching design by getting rid of what normally would be denormalized
 * data:  it stores (for example) the master data in a single normalized redis structure and
 * on cache reload, builds up the more complex structure(s) needed to perform the variety of
 * queries at the business logic level.
 */
public class PatientDataSetLogic extends LogicBase<DataServices> {

    private static final String UUID_GRP1TO4 = "00000000-0000-0000-0000-";   // 8-4-4-4-
    private static final String ZERO12       = "000000000000";

    private static final int XUUID_PREFIX_LEN = (PatientDataSet.OBJTYPE + "_" + UUID_GRP1TO4).length();

    /**
     * Any 'set' of a PDS property with this name as the key must also set
     * the value into pdsProperties
     */
    private static Set<String> pdsProperties = new HashSet<>();
    static
    {
        pdsProperties.add(PatientDataSetConstants.PATIENT_DATASRC_KEY2);
        pdsProperties.add(PatientDataSetConstants.SEQUENCE_STORE_DATASRC_KEY);
        pdsProperties.add(PatientDataSetConstants.SEQUENCE_STORE_MAP_DATASRC_KEY);
        pdsProperties.add(PatientDataSetConstants.PRIMARY_ASSIGN_AUTHORITY_KEY);
        pdsProperties.add(PatientDataSetConstants.DOCUMENT_DATASRC_KEY);
    }

    /**
     * The various types of patientDataSet management failure.
     */
    public enum FailureType {
        /**
         * for modify
         */
        NOT_EXISTING
    }

    /**
     * If patientDataSet operations fail they will throw an exception of this class.
     */
    public static class PatientDataSetException extends BaseException {
        private FailureType failureType;

        public PatientDataSetException(FailureType failureType, String details, Object... args)
        {
            super(failureType);
            super.description(details, args);
        }

        public FailureType getFailureType()
        {
            return failureType;
        }
    }

    /**
     * PropertyUtil helps manage per-patientDataSet CustomProperties
     */
    private PropertyUtil       propertyUtil;
    private PatientDataSets    pdsDao;
    private LockUtility        lockUtil;

    /**
     *
     */
    private EventLogger logger;

    /**
     * Constructor.
     */
    public PatientDataSetLogic(DataServices dataServices)
    {
        super(dataServices);

        logger = dataServices.getEventLogger("PatientDataSetLogic");
    }

    @Override
    public void postInit()
    {
        propertyUtil = new PropertyUtil(Datanames.SCOPE_PATIENT_DATASET, sysServices);
        pdsDao       = sysServices.getPatientDataSets();
        lockUtil     = new LockUtility(sysServices.getRedisOps(), "userAccounts");
    }

    /**
     * Convert a long value into an XUUID by converting the long to a zero-padded string
     * and putting that string in the lower part of a UUID.
     */
    public static XUUID patientDataSetIDFromLong(long id)
    {
        return XUUID.fromString((PatientDataSet.OBJTYPE + "_" + UUID_GRP1TO4 + zeroPad12(id)), PatientDataSet.OBJTYPE);
    }

    /**
     * Inverse of patientDataSetIDFromLong.  Null is returned if it's not a valid PDS XUUID.
     */
    public static Long longFromPatientDataSetID(XUUID pdsID)
    {
        // note that we can't just grab the lower bits numerically as we added as decimal
        // and pulling from lower bits interprets as hex

        if ((pdsID != null) && pdsID.getType().equals(PatientDataSet.OBJTYPE))
            return Long.parseLong(pdsID.toString().substring(XUUID_PREFIX_LEN));
        else
            return null;
    }

    /**
     * Activates a Patient Data Set.
     *
     * Sets the primary assign authority and creates required column families for an org.
     * Assumes all pds property definitions exist already.
     *
     * @param pds
     *          Patient Data Set
     * @param assignAuthority
     *          Org to report the PDS under
     * @throws Exception
     *          An exception will be thrown when the schema cannot be created
     */
    public void activatePatientDataSet(PatientDataSet pds, String assignAuthority) throws Exception
    {
        if (pds == null || assignAuthority == null)
            throw new IllegalArgumentException(String.format("Invalid parameter %s/%s", pds, assignAuthority));

        XUUID   pdsId    = pds.getID();
        String  pdsIdStr = normalizePdsIdToLongFormat(pdsId.getID());

        logger.info(String.format("Activating PDS %s/%s", pdsIdStr, pdsId));

        // getting pds properties from Redis
        Map<String, Object> existingProperties = getPatientDataSetProperties(pds);

        String paaName = (String) existingProperties.get(PatientDataSetConstants.PRIMARY_ASSIGN_AUTHORITY_KEY);
        if (paaName == null)
        {
            paaName = assignAuthority;
            setPdsProperty(PatientDataSetConstants.PRIMARY_ASSIGN_AUTHORITY_KEY, paaName, pdsId);
        }

        // Create afs bucket as well!
        String documentFolderName = (String) existingProperties.get(PatientDataSetConstants.DOCUMENT_DATASRC_KEY);
        if (documentFolderName == null)
        {
            documentFolderName = "document" + pdsIdStr;
            setPdsProperty(PatientDataSetConstants.DOCUMENT_DATASRC_KEY, documentFolderName, pdsId);
        }

        activateScienceCluster(pds);
        activateInternalCluster(pds);
        activateApplicationPlatformCluster(pds);

        // Customer is ready
        pds.setActive(true);
        updatePatientDataSet(pds);

        logger.info(String.format("Activation completed for PDS %s/%s", pdsIdStr, pdsId));
    }

    public void activateScienceCluster(PatientDataSet pds) throws Exception
    {
        XUUID   pdsId        = pds.getID();
        String  pdsIdStr     = normalizePdsIdToLongFormat(pdsId.getID());
        CqlCrud cqlCrud      = sysServices.getCqlCrud(PersistenceServices.CassandraCluster.SCIENCE);
        String  lock         = null;
        String  redisLockKey = "userpds-lock-" + pdsIdStr;

        if (!cqlCrud.verifySchemaConsistency())
            throw new IllegalStateException("Existing cluster - Begin - No Schema Agreement");

        try
        {
            // lock for two hours and try 5 times!!!
            lock = lockUtil.getLock(redisLockKey, 1000 * 60 * 60 * 2, 5);
            if (lock == null)
                throw new IllegalStateException("couldn't lock user account: " + pdsId);

            // getting pds properties from Redis
            Map<String, Object> existingProperties = getPatientDataSetProperties(pds);

            //
            // Create the sequence store for historical reasons.... This will eventually be deleted when we
            // complete our migration....
            //
            String oldSequenceStoreTableName = (String) existingProperties.get(PatientDataSetConstants.SEQUENCE_STORE_DATASRC_KEY);
            if (oldSequenceStoreTableName == null)
            {
                oldSequenceStoreTableName = "store" + pdsIdStr;
                setPdsProperty(PatientDataSetConstants.SEQUENCE_STORE_DATASRC_KEY, oldSequenceStoreTableName, pdsId);
            }

            boolean markHistoricalAsTrue = true;
            if (!cqlCrud.tableExists(oldSequenceStoreTableName))
            {
                if (!cqlCrud.createSizeTieredTable(oldSequenceStoreTableName))
                {
                    throw new IllegalStateException("Table creation failed.");
                }
                markHistoricalAsTrue = false;
            }

            // It is complicated to set seq stores
            String property = (String) existingProperties.get(PatientDataSetConstants.SEQUENCE_STORE_MAP_DATASRC_KEY);
            Map<String, String> seqStores = StringUtil.mapFromString(property);

            String nonInferredSequenceStoreTableName = seqStores.get("non_inferred");
            String globalSequenceStoreTableName = seqStores.get("global");
            String historicalSequenceStoreTableName = seqStores.get("historical");
            boolean updateSeqStoreCustomerProperties = false;

            if (nonInferredSequenceStoreTableName == null)
            {
                updateSeqStoreCustomerProperties = true;

                nonInferredSequenceStoreTableName = "store" + "non_inferred" + pdsIdStr;
                seqStores.put("non_inferred", nonInferredSequenceStoreTableName + "::" + System.currentTimeMillis() + "::" + "false");

                createTableIfRequired(cqlCrud, nonInferredSequenceStoreTableName);
            }

            if (globalSequenceStoreTableName == null)
            {
                updateSeqStoreCustomerProperties = true;

                globalSequenceStoreTableName = "apx_cfSequenceStore";
                seqStores.put("global", globalSequenceStoreTableName + "::" + System.currentTimeMillis() + "::" + "true" + "::" + "true");
            }

            if (historicalSequenceStoreTableName == null)
            {
                updateSeqStoreCustomerProperties = true;

                seqStores.put("historical", "store" + pdsIdStr + "::" + System.currentTimeMillis() + "::" + markHistoricalAsTrue);
            }

            if (updateSeqStoreCustomerProperties)
            {
                setPdsProperty(PatientDataSetConstants.SEQUENCE_STORE_MAP_DATASRC_KEY, StringUtil.mapToString(seqStores), pdsId);
            }
        }
        finally
        {
            if (lock != null)
                lockUtil.unlock(redisLockKey, lock);
        }

       if (!cqlCrud.verifySchemaConsistency())
           throw new IllegalStateException("Existing cluster - End - No Schema Agreement");
    }

    public void activateInternalCluster(PatientDataSet pds) throws Exception
    {
        XUUID   pdsId        = pds.getID();
        String  pdsIdStr     = normalizePdsIdToLongFormat(pdsId.getID());
        CqlCrud cqlCrud      = sysServices.getCqlCrud(PersistenceServices.CassandraCluster.INTERNAL);
        String  lock         = null;
        String  redisLockKey = "userpds-lock-" + pdsIdStr;

        if (!cqlCrud.verifySchemaConsistency())
            throw new IllegalStateException("Cluster One - Begin - No Schema Agreement");

        try
        {
            // lock for two hours and try 5 times!!!
            lock = lockUtil.getLock(redisLockKey, 1000 * 60 * 60 * 2, 5);
            if (lock == null)
                throw new IllegalStateException("couldn't lock user account: " + pdsId);

            // getting pds properties from Redis
            Map<String, Object> existingProperties = getPatientDataSetProperties(pds);

            // both patient and trace in the same table!!!
            String patientTableName = (String) existingProperties.get(PatientDataSetConstants.PATIENT_DATASRC_KEY2);
            if (patientTableName == null)
            {
                patientTableName = "patient" + pdsIdStr;
                setPdsProperty(PatientDataSetConstants.PATIENT_DATASRC_KEY2, patientTableName, pdsId);
            }

            createTableIfRequired(cqlCrud, patientTableName);
        }
        finally
        {
            if (lock != null)
                lockUtil.unlock(redisLockKey, lock);
        }

        if (!cqlCrud.verifySchemaConsistency())
            throw new IllegalStateException("Cluster One - End - No Schema Agreement");
    }

    public void activateApplicationPlatformCluster(PatientDataSet pds) throws Exception
    {
        XUUID   pdsId        = pds.getID();
        String  pdsIdStr     = normalizePdsIdToLongFormat(pdsId.getID());
        CqlCrud cqlCrud      = sysServices.getCqlCrud(PersistenceServices.CassandraCluster.APPLICATION);
        String  lock         = null;
        String  redisLockKey = "userpds-lock-" + pdsIdStr;

        if (!cqlCrud.verifySchemaConsistency())
            throw new IllegalStateException("ApplicationPlatformCluster - Begin - No Schema Agreement");

        try
        {
            // lock for two hours and try 5 times!!!
            lock = lockUtil.getLock(redisLockKey, 1000 * 60 * 60 * 2, 5);
            if (lock == null)
                throw new IllegalStateException("couldn't lock user account: " + pdsId);

            StatementGenerator statementGenerator = new StatementGenerator("rowkey", "col", "identity", "type", "dos", "d");
            String tableName = "ApplicationModel" + pdsIdStr;
            String paritionKey = "rowkey";
            String clusterKey = "col, type, dos, identity";

            FieldValueList fvl = new FieldValueListBuilder().addString("rowkey", "").addString("col", "").addString("identity", "")
                    .addString("type", "").addLong("dos", 0L).addByteBuffer("d", null).build();


            if(!cqlCrud.tableExists(tableName))
            {
                boolean success = cqlCrud.createTable(StatementGenerator.CREATE_TYPE.LEVELED, statementGenerator, tableName, fvl, paritionKey, clusterKey);
                if (!success)
                {
                    throw new IllegalStateException("Table creation failed.");
                }
            }
        }
        finally
        {
            if (lock != null)
                lockUtil.unlock(redisLockKey, lock);
        }

        if (!cqlCrud.verifySchemaConsistency())
            throw new IllegalStateException("ApplicationPlatformCluster - End - No Schema Agreement");
    }

    public static XUUID normalizePdsIdToXuuidFormat(String orgIdSt)
    {
        try
        {
            return XUUID.fromString(orgIdSt);
        }
        catch (IllegalArgumentException e)
        {
            long orgId = Long.valueOf(orgIdSt);
            return patientDataSetIDFromLong(orgId);
        }
    }

    public static String normalizePdsIdToLongFormat(String orgIdSt)
    {
        try
        {
            XUUID.fromString(orgIdSt);
            String st = orgIdSt.substring(orgIdSt.length() - 12, orgIdSt.length());
            return Long.valueOf(st).toString();
        }
        catch (IllegalArgumentException e)
        {
            return orgIdSt;
        }
    }

    public static String computeScope(String objDomain)
    {
        long   pdsIdAsLong  = Long.valueOf(normalizePdsIdToLongFormat(objDomain));
        XUUID  pdsIdAsXUUID = patientDataSetIDFromLong(pdsIdAsLong);
        String scope        = pdsIdAsXUUID.toString();

        return scope;
    }

    public static String computeScope(String objDomain, String securityScopePrefix)
    {
        long   pdsIdAsLong  = Long.valueOf(normalizePdsIdToLongFormat(objDomain));
        XUUID  pdsIdAsXUUID = patientDataSetIDFromLong(pdsIdAsLong);
        String scope        = securityScopePrefix + pdsIdAsXUUID.toString();

        return scope;
    }

    /**
     * Check if a key is part of a pds properties.
     *
     * @param key
     *          String value of key to check
     * @return
     *          true if key is part of a pds properties
     */
    public boolean isUndeletablePdsProperty(String key)
    {
        return pdsProperties.contains(key);
    }

    /**
     * Sets the pds property (which WILL get carried/pushed through to the
     * Redis-based custom properties for the PDS identified by the coID value.
     *
     * @param propertyName
     *          name of property to be set
     * @param propertyValue
     *          value of the property to be set
     * @param pdsID
     *          ID of the patient data set
     */
    public void setPdsProperty(String propertyName, String propertyValue, XUUID pdsID) {
        try
        {
            addPropertyDef(propertyName, PropertyType.STRING);
        }
        catch (Exception e)
        {
            // Log and ignore
            logger.warn(String.format("Exception found while adding pds properties for %s on %s", propertyName, pdsID));
        }

        PatientDataSet pds = getPatientDataSetByID(pdsID);

        if (pds != null)
        {
            setPatientDataSetProperty(pds, propertyName, propertyValue);
        }
        else
        {
            // Log and ignore
            logger.warn(String.format("Unable to set pds properties %s on %s", propertyName, pdsID));
        }
    }

    /**
     * Sets the primary assign authority and creates required column families for an org.
     * Assumes all pds property definitions exist already.
     */
    public void deactivatePatientDataSet(XUUID pdsID) throws Exception
    {
        PatientDataSet pds = getPatientDataSetByID(pdsID);

        if (pds != null)
        {
            pds.setActive(false);
            updatePatientDataSet(pds);
        }
    }

    /**
     * Allow us to mark the PDS active. The primary use case for this will be for external checking
     * of cassandra cf_id consistency.
     *
     * When PDS are activated - because of bugs in cassandra, schema consistency must be checked on each
     * node in certain instances. This call will allow an external call to flip the activate flag, when
     * verification is complete.
     *
     * @param pdsID
     * @throws Exception
     */
    public void markActivatePatientDataSet(XUUID pdsID) throws Exception
    {
        PatientDataSet pds = getPatientDataSetByID(pdsID);

        if (pds != null)
        {
            pds.setActive(true);
            updatePatientDataSet(pds);
        }
    }

    /**
     * Create a patientDataSet object with the given name and ID.
     */
    public PatientDataSet createPatientDataSet(String name, String description, Long id)    // patientDataSet will be marked active
    {
        if ((id != null) && (id <= 0))
            throw PatientDataSetException.badRequest("Can't create PatientDataSet; id must be > 0 but is {}", id, PatientDataSet.OBJTYPE);

        PatientDataSet patientDataSet;

        if (id != null)
        {
            patientDataSet = new PatientDataSet(name, patientDataSetIDFromLong(id));  // this is a hack constructor that overrides normal ID assignment....
            patientDataSet.setCOID(id.toString());
        }
        else
        {
            patientDataSet = new PatientDataSet(name);
        }

        patientDataSet.setDescription(description);
        pdsDao.create(patientDataSet);

        return patientDataSet;
    }

    /**
     * Find the patientDataSet with the (unique) COID and return it, null if there is no such patientDataSet.
     */
    public PatientDataSet getPatientDataSetByID(long coid)
    {
        return getPatientDataSetByID(patientDataSetIDFromLong(coid));
    }

    /**
     * Find the patientDataSet with the XUUID and return it, null if there is no such patientDataSet.
     */
    public PatientDataSet getPatientDataSetByID(XUUID xuuid)
    {
        try
        {
            return pdsDao.findPatientDataSetByID(xuuid);
        }
        catch (IllegalArgumentException x)
        {
            throw illegalArgument(x);
        }
    }

    /**
     * Return a list of all patientDataSets that are active.
     */
    public List<PatientDataSet> getAllPatientDataSets(boolean activeOnly)
    {
        return pdsDao.getAllPatientDataSets(activeOnly);
    }

    /**
     * Return a list of all patientDataSets whose name match the regular expression.  Names are
     * lowercased for the search.  Regex is a Java regular expression.
     */
    public List<PatientDataSet> findPatientDataSetsByName(String regex)
    {
        Pattern          pat = Pattern.compile(regex.toLowerCase());
        List<PatientDataSet>   all = new ArrayList<PatientDataSet>();

        for (PatientDataSet patientDataSet : pdsDao.getAllPatientDataSets(false))
        {
            if (pat.matcher(patientDataSet.getName().toLowerCase()).matches())
                all.add(patientDataSet);
        }

        return all;
    }

    /**
     * Return a list of patientDataSets that have a property of the given name with the given
     * value for the property.
     */
    public List<PatientDataSet> findPatientDataSetsByProperty(String propertyName, Object value)  // good idea, but lower priority
    {
        throw new RuntimeException("Unsupported (it was lower priority...)");
    }

    /**
     * Persist any changes to the PatientDataSet object.
     */
    public void updatePatientDataSet(PatientDataSet patientDataSet)  // name and active flag can be updated via this
    {
        if (patientDataSet.getID() == null)
            throw new PatientDataSetException(FailureType.NOT_EXISTING, "PatientDataSet has not been persisted yet");

        pdsDao.update(patientDataSet);
    }

    //!! temporary for testing as we don't delete PatientDataSets
    public void deletePatientDataSet(PatientDataSet patientDataSet)
    {
        pdsDao.delete(patientDataSet);
    }

    // ################################################################
    //  Association with Organizations
    // ################################################################

    public List<PatientDataSet> getPdsList(XUUID orgID)
    {
        return pdsDao.findPatientDataSetsByIDs(pdsDao.getPdsOwnedbyOrg(orgID));
    }

    public boolean associatePdsToID(XUUID pdsID, XUUID orgID)
    {
        PatientDataSet pds = pdsDao.findPatientDataSetByID(pdsID);

        if (pds != null)
            return associatePdsToID(pds, orgID);

        return false;
    }

    public boolean associatePdsToID(PatientDataSet pds, XUUID orgID)
    {
        if (pds != null)
        {
            XUUID pdsID = pds.getID();
            XUUID  curOrgID = pds.getOwningOrganization();

            if ((curOrgID != null) && !orgID.equals(curOrgID))
                pdsDao.disownPds(pdsID);

            if (pdsDao.ownPds(pdsID, orgID))
            {
                pds.setOwningOrganization(orgID);
                pdsDao.update(pds);

                return true;
            }
        }

        return false;
    }

    public void unassociatePdsFromID(XUUID pdsID, XUUID otherID)
    {
        PatientDataSet pds = pdsDao.findPatientDataSetByID(pdsID);

        if (pds != null)
        {
            if (pds.getOwningOrganization() != null)
            {
                pdsDao.disownPds(pdsID);
                pds.setOwningOrganization(null);
                pdsDao.update(pds);
            }
        }
    }

    public List<PatientDataSet> getUnassociatedPds()
    {
        return pdsDao.findPatientDataSetsByIDs(pdsDao.getUnownedPds());
    }

    // ################################################################
    //  PDS Properties
    // ################################################################

    /**
     * The PDS Property model allows a set of uniquely named properties that are
     * then available to have values associated with those property names added to
     * a patientDataSet object.  PDS properties also have a type that limits the types
     * of values that can be added to patientDataSet objects.
     *
     * PDS Properties have a lifetime independent of patientDataSets and their property values.
     * Actual custom property values on patientDataSets are tied to the lifetime of PDS Properties
     * as a deletion of the global PDS Property definition will delete all properties
     * of that type/name from all patientDataSets.
     */

    /**
     * Add a new custom property to the global set of properties.  The name must be unique
     * when lowercased.
     */
    public void addPropertyDef(String name, PropertyType type)   // throws exception if name.trim.tolowercase is not unique
    {
        propertyUtil.addPropertyDef(name, type);
    }

    /**
     * Returns a map from unique property name to the declared PropertyType of that property.
     */
    public Map<String, String> getPropertyDefinitions(boolean includeMeta) // <propName, propType>
    {
        return propertyUtil.getPropertyDefs(includeMeta);
    }

    /**
     * Removes the custom property definition.  This removal will cascade to a deletion of
     * property values on patientDataSet objects.
     */
    public void removePropertyDef(String name)
    {
        propertyUtil.removePropertyDef(name);
    }

    /**
     * Add a property value to the given patientDataSet.  The actual value
     * will be converted--as possible--to the declared property type.
     */
    public void setPatientDataSetProperty(PatientDataSet patientDataSet, String propertyName, String valueStr) // throws exception if name not known
    {
        propertyUtil.setEntityProperty(patientDataSet.getID(), propertyName, valueStr);
    }

    /**
     * Remove a custom property value from the given patientDataSet.
     */
    public void removePatientDataSetProperty(PatientDataSet patientDataSet, String propertyName) // throws exception if name not known
    {
        propertyUtil.removeEntityProperty(patientDataSet.getID(), propertyName);
    }

    public void removeSeqStore(PatientDataSet patientDataSet, String model)
    {
        Map<String, Object> nameToValue = getPatientDataSetProperties(patientDataSet);
        String              property    = nameToValue != null ? (String) nameToValue.get(PatientDataSetConstants.SEQUENCE_STORE_MAP_DATASRC_KEY) : null;
        Map<String, String> seqStores   = StringUtil.mapFromString(property);

        String cfSeqStore = seqStores.get(model);

        if (cfSeqStore != null)
        {
            String[] split = cfSeqStore.split("::");  // what a hack!!!!
            String   cf    = split[0];

            seqStores.remove(model);
            setPdsProperty(PatientDataSetConstants.SEQUENCE_STORE_MAP_DATASRC_KEY, StringUtil.mapToString(seqStores), patientDataSet.getID());
            services.getCqlCrud(PersistenceServices.CassandraCluster.SCIENCE).dropTable(cf);
        }
    }

    /**
     * Returns a map from patientDataSetXUUIDs to a map that contains all the name=value pairs for each patientDataSet.
     */
    public Map<XUUID, Map<String, Object>> getAllPatientDataSetProperties()                 // <PatientDataSetID, <propname, propvalue>>
    {
        return propertyUtil.getAllCustomProperties();
    }

    /**
     * Given a property name, return a map from patientDataSetXUUID to the property value for that patientDataSet.
     */
    public Map<XUUID, Object> getPatientDataSetsProperty(String propName)     // <PatientDataSetID, propvalue>
    {
        return propertyUtil.getCustomProperty(propName);
    }

    /**
     * Given a patientDataSet, return a map from property name to the property value for that patientDataSet.
     */
    public Map<String, Object> getPatientDataSetProperties(PatientDataSet patientDataSet)   // <propname, propvalue>
    {
        return propertyUtil.getEntityProperties(patientDataSet.getID());
    }

    /**
     * Central thrower of bad param exception
     */
    private BaseException illegalArgument(IllegalArgumentException x)
    {
        return PatientDataSetException.badRequest("Illegal argument {}", x.getMessage());
    }

    private static String zeroPad12(long value)
    {
        String vStr = Long.toString(value);

        return ZERO12.substring(0, 12 - vStr.length()) + vStr;
    }

    private void createTableIfRequired(CqlCrud cqlCrud, String tableName) {
        if(!cqlCrud.tableExists(tableName))
        {
            boolean success = cqlCrud.createSizeTieredTable(tableName);
            if (!success)
            {
                throw new IllegalStateException("Table creation failed.");
            }
        }
    }

}
