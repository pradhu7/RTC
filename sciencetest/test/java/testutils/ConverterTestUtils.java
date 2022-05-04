package testutils;

import com.apixio.SysServices;
import com.apixio.dao.customerproperties.CustomerProperties;
import com.apixio.dao.patient.PatientDAO;
import com.apixio.datasource.cassandra.CqlConnector;
import com.apixio.datasource.cassandra.CqlCrud;
import com.apixio.datasource.redis.RedisOps;
import com.apixio.datasource.redis.Transactions;
import com.apixio.model.converter.lib.RDFModel;
import com.apixio.model.converter.utils.SPARQLUtils;
import com.apixio.model.converter.vocabulary.OWLVocabulary;
import com.apixio.model.patient.ClinicalCode;
import com.apixio.restbase.config.TokenConfig;
import com.apixio.restbase.dao.Tokens;
import com.datastax.driver.core.ConsistencyLevel;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.vocabulary.RDF;
import org.apache.commons.io.FileUtils;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Created by jctoledo on 4/28/16.
 */
public class ConverterTestUtils {


    public static String makeStringFromInputStream(InputStream ais){
        StringBuilder sb=new StringBuilder();
        BufferedReader br = new BufferedReader(new InputStreamReader(ais));
        String read;
        try {
            while((read=br.readLine()) != null) {
                sb.append(read.trim()+"\n");
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }


    public static ClinicalCode createCC(String code, String codingSystem, String codingSystemOid, String codingSystemVersion, String displayName){
        ClinicalCode rm = new ClinicalCode();
        rm.setCode(code);
        rm.setCodingSystem(codingSystem);
        rm.setCodingSystemOID(codingSystemOid);
        rm.setCodingSystemVersions(codingSystemVersion);
        rm.setDisplayName(displayName);
        return rm;
    }


    /**
     * Verify that an patient-uuids.txt file exists in the test's resources directory
     *
     * @return true if the file exists
     */
    public static boolean fileExists(String aFileName) {
        try {
            ClassLoader cl = ConverterTestUtils.class.getClassLoader();
            File inputF = new File(cl.getResource(aFileName).getFile());
            if (countLines(inputF) == 0) {
                return false;
            }
        } catch (Exception e) {
            System.out.println(aFileName + " not found!");
            return false;
        }
        return true;
    }


    public static int countLines(File f) throws IOException {
        InputStream is = FileUtils.openInputStream(f);
        try {
            byte[] c = new byte[1024];
            int count = 0;
            int readChars = 0;
            boolean endsWithoutNewLine = false;
            while ((readChars = is.read(c)) != -1) {
                for (int i = 0; i < readChars; ++i) {
                    if (c[i] == '\n')
                        ++count;
                }
                endsWithoutNewLine = (c[readChars - 1] != '\n');
            }
            if (endsWithoutNewLine) {
                ++count;
            }
            return count;
        } finally {
            is.close();
        }
    }

    public static List<UUID> getRandomPatients(String fn, int max) {
        List<UUID> rm = new ArrayList<>();
        try {
            ClassLoader classLoader = ConverterTestUtils.class.getClassLoader();
            File inputF = new File(classLoader.getResource(fn).getFile());
            List<String> lines = FileUtils.readLines(inputF);
            Collections.shuffle(lines);
            int c = 0;
            for (String s : lines) {
                if (c == max) {
                    break;
                }
                if (s.startsWith("#") || s.length() == 0 || s == null) {
                    continue;
                }
                if (!rm.contains(UUID.fromString(s))) {
                    rm.add(UUID.fromString(s));
                    c++;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return rm;
    }

    public static double average(List<Long> list) {
        // 'average' is undefined if there are no elements in the list.
        if (list == null || list.isEmpty())
            return 0.0;
        // Calculate the summation of the elements in the list
        long sum = 0;
        int n = list.size();
        // Iterating manually is faster than using an enhanced for loop.
        for (int i = 0; i < n; i++)
            sum += list.get(i);
        // We don't want to perform an integer division, so the cast is mandatory.
        return ((double) sum) / n;
    }

    public static int countPatients(RDFModel m) {
        String qs =
                "SELECT (count(?p) AS ?count) " +
                        " WHERE {" +
                        "   ?p <" + RDF.type + "> <" + OWLVocabulary.OWLClass.Patient.uri() + ">. " +
                        "   }";
        ResultSet results = SPARQLUtils.executeSPARQLSelect(m, qs);
        while (results.hasNext()) {
            QuerySolution sol = results.next();
            Literal c = sol.getLiteral("count");
            return c.getInt();
        }
        return -1;
    }



    public static CqlCrud setupCassandra(String hosts) {
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

    public static SysServices setupSysServices(CqlCrud cc, String prefix, String host, int port) {
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

    public static PatientDAO setupPatientDao(CqlCrud cc, SysServices ss) {
        PatientDAO pdao = new PatientDAO();
        CustomerProperties cp = new CustomerProperties();
        cp.setSysServices(ss);
        pdao.setCqlCrud(cc);
        pdao.setCustomerProperties(cp);
        pdao.setLinkCF("apx_cflink_new");
        return pdao;
    }
}
