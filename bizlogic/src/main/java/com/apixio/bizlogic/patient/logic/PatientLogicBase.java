package com.apixio.bizlogic.patient.logic;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.apixio.dao.blobdao.BlobDAO;
import com.apixio.dao.customerproperties.CustomerProperties;
import com.apixio.dao.patient2.PatientDAO2;
import com.apixio.dao.utility.DaoServices;
import com.apixio.dao.utility.LinkDataUtility;
import com.apixio.dao.utility.LinkDataUtility.PdsIDAndPatientUUID;
import com.apixio.dao.utility.LinkDataUtility.PdsIDAndPatientUUIDs;
import com.apixio.datasource.cassandra.CqlCache;
import com.apixio.datasource.cassandra.CqlCrud;
import com.apixio.datasource.redis.RedisOps;
import com.apixio.datasource.utility.LockUtility;
import com.apixio.useracct.buslog.BatchLogic;
import com.apixio.useracct.buslog.PatientDataSetLogic;

public abstract class PatientLogicBase
{

    /**
     * Allows some client code to optionally do the domain translation
     */
    public interface PatientDomainTranslator
    {
        String translate(String domain) throws IOException;
    }

    /**
     *
     */
    protected PatientDAO2                        patientDAO2;
    protected BlobDAO                            blobDAO;
    protected String                             linkCF;
    protected CqlCrud                            applicationCqlCrud;
    protected CqlCache                           applicationCqlCache;
    protected CqlCrud                            internalCqlCrud;
    protected CqlCache                           internalCqlCache;
    protected RedisOps                           redisOps;
    protected LockUtility                        lockUtil;
    protected BatchLogic                         batchLogic;
    protected CustomerProperties                 customerProperties;
    protected PatientDomainTranslator            translator;
    protected com.apixio.nassembly.AssemblyLogic newAssemblyLogic;

    protected PatientLogicBase(final DaoServices daoServices)
    {
        this.patientDAO2              = daoServices.getPatientDAO2();
        this.blobDAO                  = daoServices.getBlobDAO();
        this.linkCF                   = daoServices.getLinkCF2();
        this.applicationCqlCrud       = daoServices.getApplicationCqlCrud();
        this.applicationCqlCache      = applicationCqlCrud.getCqlCache();
        this.internalCqlCrud          = daoServices.getInternalCqlCrud();
        this.internalCqlCache         = internalCqlCrud.getCqlCache();
        this.redisOps                 = daoServices.getRedisOps();
        this.lockUtil                 = new LockUtility(redisOps, "patient");
        this.batchLogic               = daoServices.getBatchLogic();
        this.customerProperties       = daoServices.getCustomerProperties();

        this.translator               = new PatientDomainTranslator()
        {
            private final CustomerProperties custProps = daoServices.getCustomerProperties();

            public String translate(String objDomain) throws IOException
            {
                try
                {
                    return custProps.getColumnFamily2(objDomain);
                }
                catch (Exception x)
                {
                    throw new IOException(x);
                }
            }
        };

        this.newAssemblyLogic = new com.apixio.nassembly.AssemblyLogicImpl(daoServices);
    }

    public UUID getPatientUUIDByDocumentUUID(UUID documentUUID) throws Exception
    {
        return LinkDataUtility.getPatientUUIDByDocumentUUID(applicationCqlCrud, documentUUID, linkCF);
    }

    public String getPdsIDByDocumentUUID(UUID documentUUID) throws Exception
    {
        return LinkDataUtility.getPdsIDByDocumentUUID(applicationCqlCrud, documentUUID, linkCF);
    }

    public PdsIDAndPatientUUID getPdsIDAndPatientUUIDByDocumentUUID(UUID documentUUID) throws Exception
    {
        return LinkDataUtility.getPdsIDAndPatientUUIDByDocumentUUID(applicationCqlCrud, documentUUID, linkCF);
    }

    public PdsIDAndPatientUUID getPdsIDAndAuthPatientUUIDdByPatientUUID(UUID patientUUID) throws Exception
    {
        return LinkDataUtility.getPdsIDAndAuthPatientUUIDdByPatientUUID(applicationCqlCrud, patientUUID, linkCF);
    }

    public PdsIDAndPatientUUIDs getPdsIDAndPatientUUIDsByPatientUUID(UUID patientUUID) throws Exception
    {
        return LinkDataUtility.getPdsIDAndPatientUUIDsByPatientUUID(applicationCqlCrud, patientUUID, linkCF);
    }

    public List<String> getBadBatches(String pdsID)
    {
        String currPdsId =  PatientDataSetLogic.normalizePdsIdToLongFormat(pdsID);

        //https://apixio.atlassian.net/browse/EN-11093
        long start = 0;

        //The end time is the latest available bad batch.
        long end = Long.MAX_VALUE;

        List<BatchLogic.UploadBatch> uploadBatches =  batchLogic.getBadUploadBatches(PatientDataSetLogic.normalizePdsIdToXuuidFormat(currPdsId), start, end);

        List<String> batchIDs = new ArrayList<>();
        if (uploadBatches == null)
            return batchIDs;

        for (BatchLogic.UploadBatch uploadBatch : uploadBatches)
        {
            batchIDs.add(uploadBatch.getName());
        }

        return batchIDs;
    }
}