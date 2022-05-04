package com.apixio.model.converter;

import com.apixio.SysServices;
import com.apixio.dao.customerproperties.CustomerProperties;
import com.apixio.dao.patient.PatientDAO;
import com.apixio.dao.patient.PatientWithTS;
import com.apixio.datasource.cassandra.CqlConnector;
import com.apixio.datasource.cassandra.CqlCrud;
import com.apixio.datasource.redis.RedisOps;
import com.apixio.datasource.redis.Transactions;
import com.apixio.model.converter.bin.converters.NewAPOToRDFModel;
import com.apixio.model.converter.bin.converters.OldAPOToNewAPO;
import com.apixio.model.converter.lib.RDFModel;
import com.apixio.model.patient.Patient;
import com.apixio.restbase.config.TokenConfig;
import com.apixio.restbase.dao.Tokens;
import com.datastax.driver.core.ConsistencyLevel;
import org.apache.commons.io.FileUtils;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.File;
import java.io.IOException;

/**
 * Created by jctoledo on 4/22/16.
 */
public class RunConverter {
    private static final String patientListFileName = "patient-uuids.txt";
    private static final String patientListWithPorP = "patient-uuids-with-problems-or-procedures.txt";
    private static final String cassandra_host_all = "54.148.81.174,54.213.141.193,54.201.203.33,54.213.120.152,54.213.19.243,54.203.11.218,54.202.255.55,54.200.209.74,54.191.235.45,54.218.125.136,54.149.123.48,54.213.115.43,54.218.176.141,54.244.193.218,54.218.80.119,54.201.113.170,54.213.57.209,54.213.240.163,54.201.41.162,54.213.216.251,54.201.114.126,54.214.141.198,54.149.222.104,54.218.135.68,54.244.63.100,54.200.165.254,54.201.141.141,54.149.110.232,54.244.201.219,52.89.224.241,54.69.187.188,54.187.33.63";
    private static final String cassandra_host = "52.89.224.241,54.69.187.188,54.187.33.63";
    private static final String redis_host = "52.11.105.117";
    private static final String redis_prefix = "productionauth-";
    private static final int redis_port = 6379;
    private static CqlCrud cqlConnector = null;
    private static SysServices sysServices = null;
    private static PatientDAO dao = null;
    private static List<UUID> patientIds = null;
    private static Map<Patient,com.apixio.model.owl.interfaces.Patient> patientMap = null;
    private static Map<com.apixio.model.owl.interfaces.Patient, RDFModel> newAPOToJenaMap = null;
    private static int sampleSize = 200;

    private static List<RDFModel> RDFModelList = new ArrayList<>();
    private static List<UUID> patientsWithPorP = new ArrayList<>();

    public static void main(String [] args) throws Exception {
        System.out.println("starting connection ...");
        cqlConnector = setupCassandra(cassandra_host);
        sysServices = setupSysServices(cqlConnector, redis_prefix, redis_host, redis_port);
        dao = setupPatientDao(cqlConnector, sysServices);
        //patientsWithPorP = getRandomPatients(patientListWithPorP, sampleSize);
        System.out.println("connection initialized ...");
        java.util.List<com.apixio.dao.patient.PatientWithTS> x = dao.getAllPartialPatients(UUID.fromString("c58aaa96-e2a0-47eb-b16a-4b12924864c1"),false);
        System.out.println(x.size());

        if(patientsWithPorP.size() == 0){
            patientsWithPorP.add(UUID.fromString("ab931c62-dade-4a6e-b365-b93cb2dc6491"));

        }
        System.out.println(patientsWithPorP);
        for (UUID u : patientsWithPorP) {
            try {
                System.out.println("fetching patients");
                //long OldToNewAPOStartTime = System.currentTimeMillis();
                List<PatientWithTS> pList = dao.getAllPartialPatients(u, false);
                NewAPOToRDFModel newAPOModel = new NewAPOToRDFModel();
                for (PatientWithTS pwts : pList) {
                    pwts.patient.setPatientId(u); // set the same id for all partials
                    com.apixio.model.owl.interfaces.Patient np = new OldAPOToNewAPO(pwts.patient).getNewPatient();
                    NewAPOToRDFModel partialPatient = new NewAPOToRDFModel(np);
                    newAPOModel.getJenaModel().add(partialPatient.getJenaModel());
                }
                RDFModel patientModel = newAPOModel.getJenaModel();
                RDFModelList.add(patientModel);
                System.out.println("over here now ");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }




    private static CqlCrud setupCassandra(String hosts) {
        CqlCrud crud = null;
        try {
            CqlConnector connector = new CqlConnector();
            connector.setBinaryPort(9042);
            connector.setKeyspaceName("apixio");
            connector.setHosts(hosts);
            connector.setMinConnections(5);
            connector.setMaxConnections(30);
            connector.setDefaultReadConsistencyLevel(ConsistencyLevel.ONE.name());
            connector.init();
            crud = new CqlCrud();
            crud.setStatementCacheEnabled(true);
            crud.setCqlConnector(connector);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return crud;
    }

    private static SysServices setupSysServices(CqlCrud cc, String prefix, String host, int port) {
        int timeout = 60000;
        SysServices ss = null;
        try {
            JedisPoolConfig jpc = new JedisPoolConfig();
            jpc.setTestWhileIdle(true);
            jpc.setTestOnBorrow(true);
            jpc.setMaxTotal(100);
            JedisPool jp = new JedisPool(jpc, host, port, timeout);
            RedisOps ro = new RedisOps(jp);
            Transactions t = new Transactions(jp);
            ro.setTransactions(t);
            Tokens tokens = new Tokens(ro, prefix, new TokenConfig());
            ss = new SysServices(ro, t, cc, tokens, "apx_cfAcl");
            //ss = new SysServices(ro, t, cc, tokens);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ss;
    }

    private static PatientDAO setupPatientDao(CqlCrud cc, SysServices ss) {
        PatientDAO pdao = new PatientDAO();
        CustomerProperties cp = new CustomerProperties();
        cp.setSysServices(ss);
        pdao.setCqlCrud(cc);
        pdao.setCustomerProperties(cp);
        pdao.setLinkCF("apx_cflink_new");
        return pdao;
    }


    private static List<UUID> getRandomPatients(String fn, int max) {
        List<UUID> rm = new ArrayList<>();
        try {
            ClassLoader classLoader = RunConverter.class.getClassLoader();
            File inputF = new File(classLoader.getResource(fn).getFile());
            List<String> lines = FileUtils.readLines(inputF);
            Collections.shuffle(lines);
            int c = 0;
            for (String s : lines) {
                if (c == max) {
                    break;
                }
                if(s.startsWith("#")){
                    continue;
                }
                rm.add(UUID.fromString(s));
                c++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return rm;
    }
}
