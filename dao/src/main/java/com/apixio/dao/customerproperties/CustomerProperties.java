package com.apixio.dao.customerproperties;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apixio.XUUID;
import com.apixio.restbase.PropertyType;
import com.apixio.restbase.DataServices;
import com.apixio.useracct.buslog.PatientDataSetConstants;
import com.apixio.useracct.buslog.PatientDataSetLogic;
import com.apixio.useracct.entity.PatientDataSet;

/**
 * CustomerProperties provides an interface for writing properties to a persistent store
 * and reading properties from a persistent store.
 *
 * Design:
 * - For each property, there will be a setter and a getter. All getters throw an Exception
 * if the value of the property is not available.
 *
 * - The data is read first from a local cache and if it not there, then it is read from
 * patient data set (implemented on top of Redis, the persistent store for properties).
 *
 * - The data is written to both patient data set (Redis) and Org Properties (the old interface
 * / MySql). The reason the data is written to MySql is to avoid porting old applications.
 *
 * - There are two modes of operation: Persistent and Non Persistent Mode. In a non persistent
 * mode, the data is kept in memory (write to and read from volatile memory) while in persistent
 * mode, the data is kept in persistent store (write to redis and MySql and read from Redis).
 *
 * - The Non Persistent mode is useful for unit testing anf migration while the Persistent Mode
 * should be used in all other situations.
 */

public class CustomerProperties
{
    private static final Logger logger = LoggerFactory.getLogger(CustomerProperties.class);

    private volatile Map<String, String> propertiesMap = new ConcurrentHashMap<>();

    private DataServices        dataServices;
    private PatientDataSetLogic patientDataSetLogic;
    private boolean             isNonPersistentMode;

    public void setDataServices(DataServices dataServices)
    {
        this.dataServices = dataServices;
        patientDataSetLogic = this.dataServices.getPatientDataSetLogic();
    }

    public void setNonPersistentMode()
    {
        isNonPersistentMode = true;
    }

    /**
     * set new patient cf given an orgId and property/table name.
     */
    public void setColumnFamily2(String property, String orgId) throws Exception
    {
        setProperty(PatientDataSetConstants.PATIENT_DATASRC_KEY2, property, orgId);
    }

    /**
     * get the new patient cf given an orgId.
     */
    public String getColumnFamily2(String orgId) throws Exception
    {
        return getProperty(PatientDataSetConstants.PATIENT_DATASRC_KEY2, orgId);
    }

    /**
     * get the trace cf given an orgId.
     */
    public String getTraceColumnFamily2(String orgId) throws Exception
    {
        return getProperty(PatientDataSetConstants.PATIENT_DATASRC_KEY2, orgId);
    }

    /**
     *  Refresh the seq store column family. Read it from Redis and
     *  update the cache
     */
    public void refreshSequenceStore(String orgId) throws Exception
    {
        refresh(PatientDataSetConstants.SEQUENCE_STORE_MAP_DATASRC_KEY, orgId);
    }

    public void setSequenceStoreColumnFamily(String property, String orgId) throws Exception
    {
        setProperty(PatientDataSetConstants.SEQUENCE_STORE_MAP_DATASRC_KEY, property, orgId);
    }

    public String getSequenceStoreColumnFamily(String orgId) throws Exception
    {
        return getProperty(PatientDataSetConstants.SEQUENCE_STORE_MAP_DATASRC_KEY, orgId);
    }

    @Deprecated
    public String getOldSequenceStoreColumnFamily(String orgId) throws Exception
    {
        return getProperty(PatientDataSetConstants.SEQUENCE_STORE_DATASRC_KEY, orgId);
    }

    /**
     * get the AFS folder given an orgId.
     */
    public String getAFSFolder(String orgId) throws Exception
    {
        return getProperty(PatientDataSetConstants.DOCUMENT_DATASRC_KEY, orgId);
    }

    /**
     * set the AFS folder given an AFS folder and an orgId.
     */
    public void setAFSFolder(String afsFolder, String orgId) throws Exception
    {
        setProperty(PatientDataSetConstants.DOCUMENT_DATASRC_KEY, afsFolder, orgId);
    }

    /**
     * get the primary assign authority given an orgId.
     */
    public String getPrimaryAssignAuthority(String orgId) throws Exception
    {
        return getProperty(PatientDataSetConstants.PRIMARY_ASSIGN_AUTHORITY_KEY, orgId);
    }

    /**
     * set the primary assign authority given a primary assign authority and an orgId.
     */
    public void setPrimaryAssignAuthority(String primaryAssignAuthority, String orgId) throws Exception
    {
        setProperty(PatientDataSetConstants.PRIMARY_ASSIGN_AUTHORITY_KEY, primaryAssignAuthority, orgId);
    }

    public void setLastPatientAssemblyMergeDownDateTime(String orgId, String category, long datetime) throws Exception
    {
        setProperty(PatientDataSetConstants.LAST_PATIENTASSEMBLY_MERGE_DOWN_DATETIME + "_" + category, Long.toString(datetime), orgId);
    }

    public long getLastPatientAssemblyMergeDownDateTime(String orgId, String category)
    {
        long lastMergeDownTime = 0;
        try
        {
            lastMergeDownTime =  Long.valueOf(getProperty(PatientDataSetConstants.LAST_PATIENTASSEMBLY_MERGE_DOWN_DATETIME + "_" + category, orgId));
        }
        catch (Exception e)
        {
            //ignore
        }
        return lastMergeDownTime;
    }

    /**
     * get the ocr resolution given an orgId.
     */
    public String getOcrResolution(String orgId) throws Exception
    {
        return getProperty(PatientDataSetConstants.OCR_RESOLUTION, orgId);
    }

    /**
     * set the ocr resolution given resolution and an orgId.
     */
    public void setOcrResolution(String resolution, String orgId) throws Exception
    {
        setProperty(PatientDataSetConstants.OCR_RESOLUTION, resolution, orgId);
    }

    private String getProperty(String propertyName, String orgIdSt) throws Exception
    {
        XUUID  pdsID = normalizeOrgIdToXuuidFormat(orgIdSt);
        String value = getPropertyFromPropertiesMap(propertyName, pdsID);

        if (value == null && !isNonPersistentMode)
        {
            synchronized(this)
            {
                value = getPropertyFromPropertiesMap(propertyName, pdsID);
                if (value == null)
                {
                    if (logger.isDebugEnabled())
                        logger.debug("Cache miss for: [" + propertyName + "," + pdsID + "]");

                    value = getPropertyFromPatientDataSet(propertyName, pdsID);
                    if (value != null)
                        savePropertyIntoPropertiesMap(propertyName, value, pdsID);
                }
            }
        }

        if (value == null)
            throw new Exception("Property: " + propertyName + " is not found for this orgId:" + orgIdSt);

        return value;
    }

    private void refresh(String propertyName, String orgIdSt) throws Exception
    {
        if (isNonPersistentMode)
            return;

        XUUID pdsID = normalizeOrgIdToXuuidFormat(orgIdSt);

        synchronized(this)
        {
            String value = getPropertyFromPatientDataSet(propertyName, pdsID);
            if (value != null)
                savePropertyIntoPropertiesMap(propertyName, value, pdsID);
        }
    }

    private String getPropertyFromPropertiesMap(String propertyName, XUUID pdsID)
    {
        return propertiesMap.get(pdsID.toString() + propertyName);
    }

    private String getPropertyFromPatientDataSet(String propertyName, XUUID pdsID)
    {
        PatientDataSet      customer    = patientDataSetLogic.getPatientDataSetByID(pdsID);
        Map<String, Object> nameToValue = customer != null ? patientDataSetLogic.getPatientDataSetProperties(customer) : null;
        String              value       = nameToValue != null ? (String) nameToValue.get(propertyName) : null;

        String logSt = String.format("getPropertyFromPatientDataSetLogic - orgIdSt: %s; propertyName: %s; valueExists: %b",  pdsID.toString(), propertyName, value != null);
        logger.info(logSt);
        return value;
    }

    public void setProperty(String propertyName, String propertyValue, String orgIdSt) throws Exception
    {
        XUUID pdsID = normalizeOrgIdToXuuidFormat(orgIdSt);

        if (!isNonPersistentMode)
        {
            persistProperty(propertyName, propertyValue, pdsID);
        }
        savePropertyIntoPropertiesMap(propertyName, propertyValue, pdsID);
    }

    private void savePropertyIntoPropertiesMap(String propertyName, String propertyValue, XUUID pdsID) throws Exception
    {
        propertiesMap.put(pdsID.toString() + propertyName, propertyValue);
    }

    private void persistProperty(String propertyName, String propertyValue, XUUID pdsID) throws Exception
    {
        try
        {
            patientDataSetLogic.addPropertyDef(propertyName, PropertyType.STRING);
        }
        catch (Exception e) {}

        PatientDataSet customer = patientDataSetLogic.getPatientDataSetByID(pdsID);

        if (customer != null)
        {
            patientDataSetLogic.setPatientDataSetProperty(customer, propertyName, propertyValue);
        }
    }

    public static XUUID normalizeOrgIdToXuuidFormat(String orgIdSt)
    {
        return PatientDataSetLogic.normalizePdsIdToXuuidFormat(orgIdSt);
    }

    public static String normalizeOrgIdToLongFormat(String orgID)
    {
        return PatientDataSetLogic.normalizePdsIdToLongFormat(orgID);
    }
}