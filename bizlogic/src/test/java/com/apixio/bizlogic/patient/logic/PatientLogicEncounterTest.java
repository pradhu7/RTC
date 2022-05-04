package com.apixio.bizlogic.patient.logic;

import au.com.bytecode.opencsv.CSVReader;
import com.apixio.bizlogic.patient.assembly.PatientAssembly;
import com.apixio.dao.customerproperties.CustomerProperties;
import com.apixio.dao.utility.DaoServices;
import com.apixio.dao.utility.DaoServicesSet;
import com.apixio.datasource.cassandra.CqlCrud;
import com.apixio.datacatalog.ExternalIdOuterClass;
import com.apixio.model.nassembly.AssemblyContext;
import com.apixio.model.nassembly.CPersistable;
import com.apixio.model.nassembly.Exchange;
import com.apixio.nassembly.AssemblyLogic;
import com.apixio.nassembly.encounter.EncounterCPersistable;
import com.apixio.nassembly.encounter.EncounterExchange;
import com.apixio.restbase.config.ConfigSet;
import com.apixio.security.Config;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.fail;


@Ignore("Integration")
public class PatientLogicEncounterTest
{
    private static final String cYamlFile = "/Users/vramkouramajian/Projects/Experiments/config/configlocal.yaml";
    private static final String csvFile = "/Users/vramkouramajian/Projects/Experiments/config/ExampleEncounters.csv";

    private static final String patientTable = "cfBackward_patient";

    private static final String pdsId = "1";
    private static final String batchId = "444";
    private static final String primaryAssignAuthority = "PRIMARY_ID";

    private ConfigSet config;
    private DaoServices daoServices;
    private CqlCrud  cqlCrudInternal;
    private CqlCrud  cqlCrudApplication;
    private PatientLogic patientLogic;
    private AssemblyLogic nAssemblyLogic;

    private AssemblyContext assemblyContextImpl = new AssemblyContext() {
        public String pdsId() {return pdsId; }
        public String batchId() {return batchId; }
        public String runId() {return UUID.randomUUID().toString();}
        public String codeVersion() {return "LIG"; }
        public long   timestamp() {return System.nanoTime();}
    };

    @Before
    public void setUp() throws Exception {
        // turn off encryption
        System.setProperty(Config.SYSPROP_PREFIX + Config.ConfigKey.CONFIG_USE_ENCRYPTION.getKey(), Boolean.FALSE.toString());

        config = ConfigSet.fromYamlFile((new File(cYamlFile)));
        daoServices = DaoServicesSet.createDaoServices(config);

        // must perform before accessing any object through dao services
        if (config.getBoolean("customerPropertiesConfig.local", true)) {
            setCustomerProperties();
        }

        daoServices.getApplicationCqlCrud().setStatementCacheEnabled(false);

        cqlCrudInternal = daoServices.getInternalCqlCrud();
        cqlCrudInternal.getCqlCache().setBufferSize(0);

        cqlCrudApplication = daoServices.getApplicationCqlCrud();
        cqlCrudApplication.getCqlCache().setBufferSize(0);

        patientLogic = new PatientLogic(daoServices);

        nAssemblyLogic = patientLogic.newAssemblyLogic;

        // createTables();
    }

    @After
    public void setAfter() throws Exception {
    }

    private void setCustomerProperties()  throws  Exception {
        CustomerProperties customerProperties =  daoServices.getCustomerProperties(); // this is a must!!!
        customerProperties.setNonPersistentMode();
        customerProperties.setColumnFamily2(patientTable, pdsId);
        customerProperties.setPrimaryAssignAuthority(primaryAssignAuthority, pdsId);
    }

    @Test
    public void testAssemblies() throws Exception {

        UUID patientId = UUID.randomUUID();

        Exchange first = createFirstEncounterExchange(patientId);
        writeLinks(first);
        writeNewAssembly(first);

        Exchange second = createSecondEncounterExchange(patientId);
        writeNewAssembly(second);

        Assert.assertNotNull(patientLogic.getMergedPatientSummaryForCategory(pdsId, patientId, PatientAssembly.PatientCategory.ENCOUNTER.getCategory()));

        Assert.assertNotNull(patientLogic.getMergedPatientSummaryForCategory(pdsId, patientId, PatientAssembly.PatientCategory.ENCOUNTER.getCategory(), primaryAssignAuthority));

        Assert.assertNotNull(patientLogic.getMergedPatientSummaryForCategory(pdsId, patientId, PatientAssembly.PatientCategory.ENCOUNTER.getCategory(), primaryAssignAuthority));
    }

    private void writeLinks(Exchange exchange) {
        try {
            UUID patientId = exchange.toApo().iterator().next().getPatientId();
            UUID authPatientId = patientId;

            // don't care for this test to write to patient table
            // String partialPatientKeyHash = UUID.randomUUID().toString();
            // patientLogic.createPartialPatientLinks(pdsId, exchange.toApo());
            // patientLogic.createPatientLinks(pdsId, patientId, partialPatientKeyHash);

            patientLogic.addPatientToLinkTable(pdsId, patientId);
            // patientLogic.addDocUUIDsToLinkTable(pdsId, patientId, Arrays.asList(exchange.toApo().getParsingDetails().iterator().next().getSourceFileArchiveUUID()));
            patientLogic.addAuthoritativePatientUUID(pdsId, patientId, authPatientId);
            patientLogic.addPatientUUIDList(pdsId, patientId, Arrays.asList(patientId));


            Thread.sleep(200);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    private Exchange createFirstEncounterExchange(UUID patientId) {
        return createEncounterExchange(patientId, true);
    }

    private Exchange createSecondEncounterExchange(UUID patientId) {
        return createEncounterExchange(patientId, false);
    }

    private Exchange createEncounterExchange(UUID patientId, boolean firstOrSecond) {
        Exchange exchange = new EncounterExchange();

        Map<String, Object> fromNameToValue = readCsvRecord(csvFile, firstOrSecond);
        fromNameToValue.putAll(exchange.preProcess(assemblyContextImpl));
        exchange.parse(fromNameToValue, assemblyContextImpl);
        ExternalIdOuterClass.ExternalId primaryEid = ExternalIdOuterClass.ExternalId.newBuilder().setId(UUID.randomUUID().toString()).setAssignAuthority(primaryAssignAuthority).build();
        exchange.setIds(patientId, primaryEid.toByteArray());

        return exchange;
    }

    private Map<String, Object> readCsvRecord(String file, boolean firstOrSecond) {
        Map<String, Object> fromNameToValue = new HashMap<>();

        FileReader fileReader = null;
        CSVReader csvReader = null;

        try {
            fileReader = new FileReader(file);
            csvReader = new CSVReader(fileReader);
            String[] headers = csvReader.readNext();

            if (!firstOrSecond) csvReader.readNext();
            String[] nextRecord  = csvReader.readNext();
            int i = 0;
            for (String cell : nextRecord) {
                fromNameToValue.put(headers[i++], cell);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            try {
                if (fileReader != null) fileReader.close();
                if (csvReader != null) csvReader.close();
            } catch (Exception e) {}

        }

        return fromNameToValue;
    }

    private void writeNewAssembly(Exchange exchange) {
        CPersistable persistable = new EncounterCPersistable();
        AssemblyLogic.AssemblyMeta meta = new AssemblyLogic.AssemblyMeta(persistable.getSchema(), pdsId, persistable.getDataTypeName());
        Map<String, Object> cluster = new HashMap() { {
            put(persistable.getSchema().getClusteringCols()[0].getName(), primaryAssignAuthority);
            put(persistable.getSchema().getClusteringCols()[1].getName(), exchange.getPrimaryEid());
        } };
        AssemblyLogic.AssemblyData data = new AssemblyLogic.AssemblyData(exchange.getCid(), cluster, null, exchange.getProtoEnvelops().iterator().next().getProtoBytes());

        try {
            nAssemblyLogic.write(meta, data);
            Thread.sleep(200);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }
}






