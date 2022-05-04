package com.apixio.dao.utility;

import com.apixio.dao.chart.DocumentIndexDao;
import com.apixio.dao.provider.ProviderDAO;
import com.apixio.dao.patientsignal.PatientSignalConfig;
import com.apixio.dao.trace1.DocumentTraceDAO1;
import com.apixio.dao.annos.AnnotationsDAO;
import com.apixio.dao.blobdao.BlobDAO;
import com.apixio.dao.customerproperties.CustomerProperties;
import com.apixio.dao.indexsearch.IndexSearchDAO;
import com.apixio.dao.patient2.PatientDAO2;
import com.apixio.dao.seqstore.SeqStoreDAO;
import com.apixio.dao.seqstore.store.AddressStore;
import com.apixio.dao.seqstore.store.PathValueStore;
import com.apixio.dao.seqstore.store.QueryStore;
import com.apixio.dao.seqstore.store.SubjectPathStore;
import com.apixio.dao.seqstore.store.SubjectPathValueStore;
import com.apixio.dao.seqstore.store.SubjectStore;
import com.apixio.dao.seqstore.utility.SeqStoreCFUtility;
import com.apixio.dao.storage.ApixioStorage;
import com.apixio.datasource.afs.ApixioFS;
import com.apixio.datasource.cassandra.CqlCache;
import com.apixio.datasource.cassandra.CqlCrud;
import com.apixio.datasource.cassandra.ThreadLocalCqlCache;
import com.apixio.datasource.elasticSearch.ElasticCrud;
import com.apixio.datasource.s3.S3Connector;
import com.apixio.datasource.s3.S3Ops;
import com.apixio.logger.EventLogger;
import com.apixio.restbase.DataServices;
import com.apixio.restbase.config.ConfigSet;
import com.apixio.restbase.DaoBase;
import com.apixio.restbase.config.ConfigUtil;
import com.apixio.utility.PropertyHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public class DaoServices extends DataServices
{
    private static final Logger logger = LoggerFactory.getLogger(DaoServices.class);

    // Config Sections
    private static final String cCustomerPropertiesConfig = "customerPropertiesConfig";
    private static final String cPropertyHelperConfig     = "propertyHelperConfig";
    private static final String cStorageConfig            = "storageConfig";
    private static final String cDaoConfig                = "daoConfig";
    private static final String cPatientAnnoConfig        = "patientAnnoConfigV1";
    private static final String cLoggingConfig            = "loggingConfig";

    // customerPropertiesConfig
    private static final String  cLocal  = "local";

    // propertyHelperConfig
    private static final String cPrefix = "prefix";

    // storageConfig
    private static final String cAccessKey                  = "s3Config.accessKey";
    private static final String cSecretKey                  = "s3Config.secretKey";
    private static final String cRegion                     = "s3Config.region";
    private static final String cAccessKeyUnencrypted       = "s3Config.accessKeyUnencrypted";
    private static final String cSecretKeyUnencrypted       = "s3Config.secretKeyUnencrypted";
    private static final String cS3ConnectionTimeoutKey     = "s3Config.connectionTimeoutMs";
    private static final String cS3SocketTimeoutKey         = "s3Config.socketTimeoutMs";

    private static final String cMountPoint                 = "apixioFSConfig.mountPoint";
    private static final String cFromStorageType            = "apixioFSConfig.fromStorageType";
    private static final String cToStorageTypes             = "apixioFSConfig.toStorageTypes";

    // daoConfig
    private static final String cBufferSize                  = "cqlCacheConfig.bufferSize";
    private static final String cLink2                       = "globalTableConfig.link2"; // after migration it will renamed as the original one!!!
    private static final String assemblyDataType             = "globalTableConfig.assemblyDataType";

    // used by DocumentTraceDao
    private static final String traceTtlFromNowInSecs       = "traceConfig.ttlFromNowInSec";

    // used by patient signal eventing
    private static final String cPatientSignalBrokers       = "patientSignal.brokers";
    private static final String cPatientSignalTopic         = "patientSignal.topic";
    private static final String cPatientSignalTopicPrefix   = "patientSignal.topic_prefix";

    // used for annotation
    private static final String cSeq                         = "globalTableConfig.seq";

    private static final String cIndex                       = "globalTableConfig.index";
    private static final String cPaths                       = "seqStoreConfig.paths";

    //per tagType paths definitions...
    private static final String cAnnotationsPaths            = "seqStoreConfig.annotation_paths";

    // will be removed one day
    private static final String cNonInferredPaths            = "seqStoreConfig.noninferred_paths";
    private static final String cInferredPaths               = "seqStoreConfig.inferred_paths";

    // will be removed one day
    private static final String cQueryStoreBatchSize         = "queryStore.batchSize";

    // will be removed one day
    private static final String sequenceStoreExecutorThreadPoolSize = "seqStoreConfig.executorThreadPoolSize";

    private ConfigSet  customerPropertiesConfig;
    private ConfigSet  propertyHelperConfig;
    private ConfigSet  storageConfig;
    private ConfigSet  daoConfig;
    private ConfigSet  patientAnnoConfig;

    private volatile boolean   customerPropertiesAdded;
    private CustomerProperties customerProperties;

    private volatile boolean   propertyHelperAdded;
    private PropertyHelper     propertyHelper;

    private S3Ops              s3Ops;
    private volatile boolean   s3OpsAdded;

    private volatile boolean   apixioStorageAdded;
    private ApixioStorage      apixioStorage;

    private volatile boolean   blobDAOAdded;
    private BlobDAO            blobDAO;

    private volatile boolean   cqlCacheAdded;             // will be used for science (seq store tables)
    private volatile boolean   internalCqlCacheAdded;
    private volatile boolean   applicationCqlCacheAdded;

    private volatile boolean   providerDaoAdded;
    private ProviderDAO        providerDAO;

    // after migration it will renamed as the original one!!!
    private volatile boolean   oneToManyStore1Added;
    private OneToManyStore     oneToManyStore1;

    // will be removed after migration
    private volatile boolean   patientDAOAdded;

    // after migration it will renamed as the original one!!!
    private volatile boolean   patientDAO2Added;
    private PatientDAO2        patientDAO2;

    // after migration it will renamed as the original one!!!
    private volatile boolean   documentTraceDAO1Added;
    private DocumentTraceDAO1  documentTraceDAO1;

    // will be used one day only for annotation
    private volatile boolean   seqStoreDAOAdded;
    private SeqStoreDAO        seqStoreDAO;

    // after migration it will renamed as the original one!!!
    private volatile boolean   indexSearchDAO2Added;
    private IndexSearchDAO     indexSearchDAO2;

    private volatile boolean   newAssemblyDAOAdded;
    private com.apixio.dao.nassembly.AssemblyDAO newAssemblyDAO;

    //
    private volatile boolean   patientAnnoDAOAdded;
    private AnnotationsDAO     patientAnnoDAO;

    private volatile boolean   documentIndexDAOAdded;
    private DocumentIndexDao   documentIndexDao;

    private volatile boolean   patientSignalConfigAdded;
    private PatientSignalConfig patientSignalConfig;

    public DaoServices(DaoBase daoBase, ConfigSet configuration)
    {
        // initialize and hook data services.
        super(daoBase, configuration);
        doPostInit();

        customerPropertiesConfig  = configuration.getConfigSubtree(cCustomerPropertiesConfig);
        propertyHelperConfig      = configuration.getConfigSubtree(cPropertyHelperConfig);
        storageConfig             = configuration.getConfigSubtree(cStorageConfig);
        daoConfig                 = configuration.getConfigSubtree(cDaoConfig);
        patientAnnoConfig         = configuration.getConfigSubtree(cPatientAnnoConfig);
    }

    @Override
    public void close()
    {
        super.close();

        if (s3Ops != null)
            s3Ops.close();
    }

    public PatientSignalConfig getPatientSignalConfig()
    {
        if(!patientSignalConfigAdded)
            addPatientSignalConfig();

        return patientSignalConfig;
    }

    public CustomerProperties getCustomerProperties()
    {
        if (!customerPropertiesAdded)
            addCustomerProperties();

        return customerProperties;
    }

    public PropertyHelper getPropertyHelper()
    {
        if (!propertyHelperAdded)
            addPropertyHelper();

        return propertyHelper;
    }

    public S3Ops getS3Ops()
    {
        if (!s3OpsAdded)
            addS3Ops();

        return s3Ops;
    }

    public ApixioStorage getApixioStorage()
    {
        if (!apixioStorageAdded)
            addApixioStorage();

        return apixioStorage;
    }

    public BlobDAO getBlobDAO()
    {
        if (!blobDAOAdded)
            addBlobDAO();

        return blobDAO;
    }

    public AnnotationsDAO getPatientAnnotationsDAO()
    {
        if (!patientAnnoDAOAdded)
            addPatientAnnoDAO();

        return patientAnnoDAO;
    }

    // cluster zero. After migration it will be used only by science
    public CqlCrud getScienceCqlCrud()
    {
        CqlCrud cqlCrud = super.getCqlCrud(CassandraCluster.SCIENCE);

        if (!cqlCacheAdded)
        {
            addNewCqlCacheToCqlCrud(cqlCrud);
            cqlCacheAdded = true;
        }

        return cqlCrud;
    }

    public CqlCrud getInternalCqlCrud()
    {
        CqlCrud cqlCrud = super.getCqlCrud(CassandraCluster.INTERNAL);

        if (!internalCqlCacheAdded)
        {
            addNewCqlCacheToCqlCrud(cqlCrud);
            internalCqlCacheAdded = true;
        }

        return cqlCrud;
    }

    public CqlCrud getApplicationCqlCrud()
    {
        CqlCrud cqlCrud = super.getCqlCrud(CassandraCluster.APPLICATION);

        if (!applicationCqlCacheAdded)
        {
            addNewCqlCacheToCqlCrud(cqlCrud);
            applicationCqlCacheAdded = true;
        }

        return cqlCrud;
    }

    public CqlCrud getGenericCqlCrud(CassandraCluster cass)
    {
        CqlCrud cqlCrud = super.getCqlCrud(cass);

        addNewCqlCacheToCqlCrud(cqlCrud);

        return cqlCrud;
    }

    public ElasticCrud getChartSpaceElasticCrud() throws Exception
    {
        ElasticCrud chartSpaceCrud = super.getElasticCrud(ElasticCluster.CHART_SPACE);

        return chartSpaceCrud;
    }

    public ProviderDAO getProviderDAO()
    {
        if (!providerDaoAdded)
            addProviderDAO();

        return providerDAO;
    }

    // cluster one. After migration it will be renamed as the original one
    public OneToManyStore getOneToManyStore1()
    {
        if (!oneToManyStore1Added)
            addOneToManyStore1();

        return oneToManyStore1;
    }

    // cluster one. After migration it will be renamed as the original one
    public PatientDAO2 getPatientDAO2()
    {
        if (!patientDAO2Added)
            addPatientDAO2();

        return patientDAO2;
    }

    // cluster one. After migration it will be renamed as the original one
    public DocumentTraceDAO1 getDocumentTraceDAO1()
    {
        if (!documentTraceDAO1Added)
            addDocumentTraceDAO1();

        return documentTraceDAO1;
    }

    // cluster zero. Will be used only for annotation at some point
    public SeqStoreDAO getSeqStoreDAO()
    {
        if (!seqStoreDAOAdded)
            addSeqStoreDAO();

        return seqStoreDAO;
    }

    // cluster Two. After migration it will be renamed as the original one
    public IndexSearchDAO getIndexSearchDAO2()
    {
        if (!indexSearchDAO2Added)
            addIndexSearchDAO2();

        return indexSearchDAO2;
    }

    public com.apixio.dao.nassembly.AssemblyDAO getNewAssemblyDAO()
    {
        if (!newAssemblyDAOAdded)
            addNewAssemblyDAO();

        return newAssemblyDAO;
    }

    public DocumentIndexDao getDocumentIndexDAO() throws Exception
    {
        if (!documentIndexDAOAdded)
            addDocumentIndexDAO();

        return documentIndexDao;
    }

    private synchronized void addPatientSignalConfig()
    {
        if(!patientSignalConfigAdded)
        {
            patientSignalConfig = new PatientSignalConfig();
            patientSignalConfig.setTopic(daoConfig.getString(cPatientSignalTopic));
            patientSignalConfig.setServers(daoConfig.getString(cPatientSignalBrokers));
            patientSignalConfig.setTopicPrefix(daoConfig.getString(cPatientSignalTopicPrefix));

            patientSignalConfigAdded = true;
        }
    }

    private synchronized void addCustomerProperties()
    {
        if (!customerPropertiesAdded)
        {
            customerProperties = new CustomerProperties();
            if (customerPropertiesConfig == null || !customerPropertiesConfig.getBoolean(cLocal, false))
                customerProperties.setDataServices(this);

            customerPropertiesAdded = true;
        }
    }

    private synchronized void addPropertyHelper()
    {
        if (!propertyHelperAdded)
        {
            propertyHelper = new PropertyHelper();
            propertyHelper.setTransactions(getRedisTransactions());
            propertyHelper.setRedisOps(getRedisOps());
            propertyHelper.setPrefix(propertyHelperConfig.getString(cPrefix));

            propertyHelperAdded = true;
        }
    }

    private void addS3Ops()
    {
        if (!s3OpsAdded)
        {
            S3Connector s3Connector = new S3Connector();
            String      val;

            if ((val = storageConfig.getString(cAccessKey, null)) != null && !val.isEmpty())
                s3Connector.setAccessKey(val);
            else if ((val = storageConfig.getString(cAccessKeyUnencrypted, null)) != null && !val.isEmpty())
                s3Connector.setUnencryptedAccessKey(val);
            else
                logger.warn("No AWS AccessKey found. Going to use iam role.");
                // throw new IllegalStateException("Expected configuration value for " + cAccessKey);

            if ((val = storageConfig.getString(cSecretKey, null)) != null && !val.isEmpty())
                s3Connector.setSecretKey(val);
            else if ((val = storageConfig.getString(cSecretKeyUnencrypted, null)) != null && !val.isEmpty())
                s3Connector.setUnencryptedSecretKey(val);
            else
                logger.warn("No AWS SecretKey found. Going to use iam role");
                // throw new IllegalStateException("Expected configuration value for " + cSecretKey);

            if ((val = storageConfig.getString(cRegion, null)) != null && !val.isEmpty())
                s3Connector.setRegion(val);
            else
                logger.warn("No AWS Region found. Using default region (us-west-2)");

            s3Connector.setConnectionTimeoutMs(storageConfig.getInteger(cS3ConnectionTimeoutKey, -1));
            s3Connector.setSocketTimeoutMs(storageConfig.getInteger(cS3SocketTimeoutKey, -1));
            s3Connector.init();

            final ConfigSet loggingConfigSet = storageConfig.getConfigSubtree(cLoggingConfig);
            if (loggingConfigSet != null)
            {
                EventLogger eventLogger = ConfigUtil.getEventLogger(loggingConfigSet, null);
                S3Ops.setEventLogger(eventLogger);
            }

            s3Ops = new S3Ops();
            s3Ops.setS3Connector(s3Connector);

            s3OpsAdded = true;
        }
    }

    private void addApixioStorage()
    {
        if (!apixioStorageAdded)
        {
            ApixioFS apxFs = new ApixioFS();
            apxFs.setS3Ops(getS3Ops());

            apixioStorage = new ApixioStorage();
            apixioStorage.setApixioFS(apxFs);
            apixioStorage.setToStorageTypes(storageConfig.getString(cToStorageTypes));
            apixioStorage.setFromStorageType(storageConfig.getString(cFromStorageType));
            apixioStorage.setMountPoint(storageConfig.getString(cMountPoint));
            apixioStorage.setCustomerProperties(getCustomerProperties());

            apixioStorageAdded = true;
        }
    }

    private void addBlobDAO()
    {
        if (!blobDAOAdded)
        {
            blobDAO = new BlobDAO();
            blobDAO.setApixioStorage(getApixioStorage());

            blobDAOAdded = true;
        }
    }

    private void addPatientAnnoDAO()
    {
        if (!patientAnnoDAOAdded)
        {
            patientAnnoDAO = new AnnotationsDAO(patientAnnoConfig, getS3Ops());
            patientAnnoDAOAdded = true;
        }
    }

    private void addNewCqlCacheToCqlCrud(CqlCrud cqlCrud)
    {
        CqlCache cqlCache = new ThreadLocalCqlCache();
        cqlCache.setBufferSize(daoConfig.getInteger(cBufferSize));
        cqlCache.setCqlCrud(cqlCrud);

        cqlCrud.setCqlCache(cqlCache);
    }

    private void addOneToManyStore1()
    {
        if (!oneToManyStore1Added)
        {
            oneToManyStore1 = new OneToManyStore();
            oneToManyStore1.setCqlCrud(getInternalCqlCrud());

            oneToManyStore1Added = true;
        }
    }

    private void addProviderDAO() {
        if (!providerDaoAdded)
        {
            providerDAO = new ProviderDAO();
            providerDAO.init(getJdbc("provider"));

            providerDaoAdded = true;
        }
    }

    private void addPatientDAO2()
    {
        if (!patientDAO2Added)
        {
            patientDAO2 = new PatientDAO2();
            patientDAO2.setCqlCrud(getInternalCqlCrud());
            patientDAO2.setOneToManyStore(getOneToManyStore1());

            patientDAO2Added = true;
        }
    }

    private void addDocumentTraceDAO1()
    {
        if (!documentTraceDAO1Added)
        {
            documentTraceDAO1 = new DocumentTraceDAO1();
            documentTraceDAO1.setCqlCrud(getInternalCqlCrud());
            documentTraceDAO1.setLinkCF(getLinkCF2());
            documentTraceDAO1.setOneToManyStore(getOneToManyStore1());
            documentTraceDAO1.setCustomerProperties(getCustomerProperties());

            String ttl;
            if ((ttl = daoConfig.getString(traceTtlFromNowInSecs, null)) != null && !ttl.isEmpty())
                documentTraceDAO1.setTtlFromNowInSec(Long.parseLong(ttl));
            else
                logger.warn("No ttl set for document traces");

            documentTraceDAO1Added = true;
        }
    }

    private void addSeqStoreDAO()
    {
        if (!seqStoreDAOAdded)
        {
            SubjectStore subjectStore = new SubjectStore();
            subjectStore.setCqlCrud(getScienceCqlCrud());

            PathValueStore pathValueStore = new PathValueStore();
            pathValueStore.setCqlCrud(getScienceCqlCrud());

            SubjectPathValueStore subjectPathValueStore = new SubjectPathValueStore();
            subjectPathValueStore.setCqlCrud(getScienceCqlCrud());

            SubjectPathStore subjectPathStore = new SubjectPathStore();
            subjectPathStore.setCqlCrud(getScienceCqlCrud());

            AddressStore addressStore = new AddressStore();
            addressStore.setCqlCache(getScienceCqlCrud());

            QueryStore queryStore = new QueryStore(daoConfig.getInteger(cQueryStoreBatchSize, 1000));
            queryStore.setCqlCrud(getScienceCqlCrud());

            SeqStoreCFUtility seqStoreCFUtility = new SeqStoreCFUtility();
            seqStoreCFUtility.setCustomerProperties(getCustomerProperties());
            seqStoreCFUtility.setScienceCqlCrud(getScienceCqlCrud());
            seqStoreCFUtility.setApplicationCqlCrud(getApplicationCqlCrud());
            seqStoreCFUtility.setLinkCF(getLinkCF2());
            seqStoreCFUtility.setGlobalCF(daoConfig.getString(cSeq));

            seqStoreDAO = new SeqStoreDAO(daoConfig.getInteger(sequenceStoreExecutorThreadPoolSize, 5));
            seqStoreDAO.setSeqStoreCFUtility(seqStoreCFUtility);
            seqStoreDAO.setCqlCrud(getScienceCqlCrud());
            seqStoreDAO.setSubjectStore(subjectStore);
            seqStoreDAO.setPathValueStore(pathValueStore);
            seqStoreDAO.setSubjectPathValueStore(subjectPathValueStore);
            seqStoreDAO.setSubjectPathStore(subjectPathStore);
            seqStoreDAO.setAddressStore(addressStore);
            seqStoreDAO.setQueryStore(queryStore);
            seqStoreDAO.setPaths(daoConfig.getString(cPaths));
            seqStoreDAO.setPaths(SeqStoreDAO.TagType.Annotation, daoConfig.getString(cAnnotationsPaths, null));
            seqStoreDAO.setPaths(SeqStoreDAO.TagType.NonInferred, daoConfig.getString(cNonInferredPaths, null));
            seqStoreDAO.setPaths(SeqStoreDAO.TagType.Inferred, daoConfig.getString(cInferredPaths, null));

            seqStoreDAOAdded = true;
        }
    }

    private void addIndexSearchDAO2()
    {
        if (!indexSearchDAO2Added)
        {
            indexSearchDAO2 = new IndexSearchDAO();
            indexSearchDAO2.setCqlCrud(getApplicationCqlCrud(), true);
            indexSearchDAO2.setIndexCF(daoConfig.getString(cIndex));

            indexSearchDAO2Added = true;
        }
    }

    private void addNewAssemblyDAO()
    {
        if (!newAssemblyDAOAdded)
        {
            newAssemblyDAO = new com.apixio.dao.nassembly.AssemblyDAOImpl();
            newAssemblyDAO.setDrivers(new HashMap<String, Object>() {{
            put("application", getApplicationCqlCrud());
            put("internal", getInternalCqlCrud());
            }});

            newAssemblyDAOAdded = true;
        }
    }

    private void addDocumentIndexDAO () throws Exception
    {
        if (!documentIndexDAOAdded)
        {
            documentIndexDao = new DocumentIndexDao(getChartSpaceElasticCrud());
            documentIndexDAOAdded = true;
        }
    }

    // cluster two!!!
    public String getLinkCF2()
    {
        return daoConfig.getString(cLink2);
    }

    public String getNewAssemblyDataType()
    {
        return daoConfig.getString(assemblyDataType);
    }
}