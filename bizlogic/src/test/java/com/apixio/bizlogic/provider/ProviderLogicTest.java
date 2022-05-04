package com.apixio.bizlogic.provider;

import com.apixio.dao.provider.ProviderDAO;
import com.apixio.dao.provider.ValueAndIsClaimedCounts;
import com.apixio.dao.utility.sql.SqlOffsetIterator;
import com.apixio.dao.utility.sql.SqlOrdering;
import com.apixio.datasource.springjdbc.JdbcDS;
import com.apixio.model.prospective.DistinctProviders;
import com.apixio.model.prospective.ProgramPerformance;
import com.apixio.model.prospective.ProviderPerformance;
import com.apixio.model.prospective.ProviderResponse;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Ignore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
Shell Script to run this test from command line

m2="$HOME/.m2/repository"
cp=$(echo \
target/classes target/test-classes \
$m2/org/slf4j/slf4j-api/1.7.30/slf4j-api-1.7.30.jar \
$m2/commons-logging/commons-logging/1.2/commons-logging-1.2.jar \
$m2/org/apache/commons/commons-dbcp2/2.2.0/commons-dbcp2-2.2.0.jar \
$m2/org/apache/commons/commons-pool2/2.5.0/commons-pool2-2.5.0.jar \
$m2/org/apache/commons/commons-lang3/3.4/commons-lang3-3.4.jar \
$m2/org/springframework/spring-beans/3.2.13.RELEASE/spring-beans-3.2.13.RELEASE.jar \
$m2/org/springframework/spring-core/3.2.13.RELEASE/spring-core-3.2.13.RELEASE.jar \
$m2/org/springframework/spring-jdbc/3.2.13.RELEASE/spring-jdbc-3.2.13.RELEASE.jar \
$m2/org/springframework/spring-tx/3.2.13.RELEASE/spring-tx-3.2.13.RELEASE.jar \
$m2/org/postgresql/postgresql/42.2.2/postgresql-42.2.2.jar \
$m2/junit/junit/4.12/junit-4.12.jar \
$m2/mysql/mysql-connector-java/8.0.11/mysql-connector-java-8.0.11.jar \
$m2/org/scala-lang/scala-library/2.11.12/scala-library-2.11.12.jar \
../model/target/*.jar \
../ds/target/*.jar \
../dao/target/*.jar \
../entity-common/target/*.jar \
| sed 's/ /:/g')
java -cp $cp com.apixio.bizlogic.provider.ProviderLogicTest
 */
@Ignore("CommandLine")
public class ProviderLogicTest {

    private final ProviderLogic logic;
    final static String ramoEN10092TestProjecct = "PRPROSPECTIVE_b21ac593-069d-45af-ab84-6457b1dee669";
    final static List<String> ramoEN10092ProviderNameList = Collections.singletonList("Robin Febres");
    final static String projectId = "fakeProject";

    final static ProviderResponse pv1 = new ProviderResponse("00217597-a06a-4afb-a9cc-b0a82e8e85c5",
            projectId,
            "1723",
            "11-HCCV24",
            (float) 0.307,
            "Neoplasm",
            "rejected",
            "253857214",
            "Green Field Doctors",
            "Jarrett Gingell",
            true,
            (float) 0.307,
            false,
            0L,
            0L,
            DateTime.now().getMillis());

    final static ProviderResponse pv2 = new ProviderResponse("01a73d58-48af-4995-a966-822682c05f62",
            projectId,
            "1723",
            "85-HCCV24",
            (float) 0.331,
            "Heart",
            "accepted",
            "253857214",
            "Green Field Doctors",
            "Jarrett Gingell",
            false,
            (float) 0.331,
            false,
            1591874970000L,
            1602874970000L,
            DateTime.now().getMillis());

    final static List<ProviderResponse> responses = Arrays.asList(pv1, pv2);

    private ProviderLogicTest() {
        JdbcDS jdbc = new JdbcDS(makeConfig());
        ProviderDAO dao = new ProviderDAO();
        dao.init(jdbc);
        this.logic = new ProviderLogic(dao);
    }

    public static void main(String[] args) throws AssertionError {
        ProviderLogicTest tester = new ProviderLogicTest();
        System.out.println("Starting Mapping Logic Tests");

        tester.cleanDB();
        tester.insertRows();
        tester.updateRows();

        tester.testAllProviders();

        tester.testTotalOppsCount();
        tester.testTotalPatientCount();
        tester.testDiseaseCategoryByClaimed();
        tester.testRespondedByClaimed();
        tester.testDecisionsByClaimed();
        tester.testRafByClaimedAndDecision();
        tester.testRafByClaimedAndReportable();

        tester.testProviderPerformance();
        tester.testProviderPerformanceForRamoTest();
        tester.testProgramPerformance();

        tester.cleanDB();
    }


    ////////////////// DB init and cleanup ///////////////
    private void insertRows() {
        logic.insertRows(responses);
    }

    private void updateRows() {
        logic.updateRows(responses);
    }

    private void cleanDB() {
        logic.deleteRows(responses);
    }

    ///////////////// Tests /////////////////////////

    final static List<String> emptyStringList = new ArrayList<>();

    final static List<String> providerNameList = Collections.singletonList("Jarrett Gingell");
    final static List<String> providerGroupList = Collections.singletonList("Green Field Doctors");

    private void testAllProviders() throws AssertionError {
        System.out.println("\t Starting All Providers Test");
        List<DistinctProviders> providers = logic.getAllProviders(projectId);
        Assert.assertEquals(1, providers.size());
        System.out.println("\t All Providers Test -- SUCCESS");
    }

    private void testTotalOppsCount() throws AssertionError {
        System.out.println("\t Starting Total Opps Test");
        long count = logic.totalOppsCount(projectId, providerNameList, emptyStringList);
        Assert.assertEquals(2, count);
        System.out.println("\t Total Opps Test -- SUCCESS");
    }

    private void testTotalPatientCount() {
        System.out.println("\t Starting Total Patient Test");
        long count = logic.totalPatientCount(projectId, emptyStringList, providerGroupList);
        Assert.assertEquals(2, count);
        System.out.println("\t Total Patient Test -- SUCCESS");
    }

    private void testDiseaseCategoryByClaimed() {
        System.out.println("\t Starting Disease Categories Test");
        List<ValueAndIsClaimedCounts> categories = logic.diseaseCategoryByClaimed(projectId, providerNameList, providerGroupList);
        Assert.assertTrue(categories.size() > 0);
        System.out.println("\t Disease Categories Test -- SUCCESS");
    }

    private void testRespondedByClaimed() {
        System.out.println("\t Starting Response Date Test");
        List<ValueAndIsClaimedCounts> categories = logic.respondedByClaimed(projectId, providerNameList, providerGroupList);
        Assert.assertTrue(categories.size() > 0);
        System.out.println("\t Response Date Test -- SUCCESS");
    }

    private void testDecisionsByClaimed() {
        System.out.println("\t Starting Decisions Test");
        List<ValueAndIsClaimedCounts> categories = logic.decisionsByClaimed(projectId, providerNameList, providerGroupList);
        Assert.assertTrue(categories.size() > 0);
        System.out.println("\t Decisions Test -- SUCCESS");
    }

    private void testRafByClaimedAndDecision() {
        System.out.println("\t Starting Raf by Decision Test");
        List<ValueAndIsClaimedCounts> categories = logic.rafByClaimedAndDecision(projectId, emptyStringList, emptyStringList);
        Assert.assertTrue(categories.size() > 0);
        System.out.println("\t Raf by Decision Test -- SUCCESS");
    }

    private void testRafByClaimedAndReportable() {
        System.out.println("\t Starting Raf By Reportable Test");
        List<ValueAndIsClaimedCounts> categories = logic.rafByClaimedAndReportable(projectId, emptyStringList, emptyStringList);
        Assert.assertTrue(categories.size() > 0);
        System.out.println("\t Raf By Reportable Test -- SUCCESS");
    }

    private void testProviderPerformance() {
        System.out.println("\t Starting Provider Performance Test");
        List<SqlOrdering> sortFields = Collections.singletonList(new SqlOrdering("openRaf_count", true));
        SqlOffsetIterator<ProviderPerformance> iterator = logic.providerPerformanceIterator(projectId, providerNameList, providerGroupList, sortFields, 1, 0);
        List<ProviderPerformance> performances = new ArrayList<>();
        while (iterator.hasNext()) {
            performances.addAll(iterator.next());
        }
        Assert.assertEquals(1, performances.size());
        double openRaf = performances.stream().mapToDouble(pp -> Double.parseDouble(String.valueOf(pp.openRaf()))).sum(); // Weird rounding from float to double
        Assert.assertEquals(0.331, openRaf, 0.0);
        Assert.assertEquals(performances.get(0).projectId(), projectId);
        System.out.println("\t Provider Performance Test -- SUCCESS");
    }

    private void testProviderPerformanceForRamoTest() {
        System.out.println("\t Starting Provider Performance Test");
        List<SqlOrdering> sortFields = Collections.singletonList(new SqlOrdering("openRaf_count", true));

        SqlOffsetIterator<ProviderPerformance> iterator = logic.providerPerformanceIterator(ramoEN10092TestProjecct, ramoEN10092ProviderNameList, emptyStringList, sortFields, 1, 0);
        List<ProviderPerformance> performances = new ArrayList<>();
        while (iterator.hasNext()) {
            performances.addAll(iterator.next());
        }
        ProviderPerformance res = performances.get(0);
//        Assert.assertEquals(1, performances.size());
        System.out.println("\n\n-----");
        System.out.print("providerName -- ");
        System.out.println(res.providerName());
        System.out.print("providerGroup -- ");
        System.out.println(res.providerGroup());
        System.out.print("npi -- ");
        System.out.println(res.npi());
        System.out.print("total -- ");
        System.out.println(res.total());
        System.out.print("delivered -- ");
        System.out.println(res.delivered());
        System.out.print("accepted -- ");
        System.out.println(res.accepted());
        System.out.print("rejected -- ");
        System.out.println(res.rejected());
        System.out.print("snoozed -- ");
        System.out.println(res.snooze());
        System.out.print("claimed -- ");
        System.out.println(res.claimed());
        System.out.print("openRaf -- ");
        System.out.println(res.openRaf());
        System.out.print("responseCount -- ");
        System.out.println(res.responseCount());
        System.out.print("responseRate -- ");
        System.out.println(res.responseRate());
        System.out.print("acceptRate -- ");
        System.out.println(res.acceptRate());

        System.out.println("-----\n\n");

        System.out.println("\t Provider Performance Test -- SUCCESS");
    }

    private void testProgramPerformance() {
        System.out.println("\t Starting program performance Test");
        List<ProgramPerformance> categories = logic.getProgramPerformance(ramoEN10092TestProjecct, ramoEN10092ProviderNameList, emptyStringList);
        System.out.println("\n\n---------");

        ProgramPerformance res = categories.get(0);

        System.out.print("claimed_count -- ");
        System.out.println(res.claimed_count());
        System.out.print("claimRaf_count -- ");
        System.out.println(res.claimRaf_count());
        System.out.print("open_count -- ");
        System.out.println(res.open_opp_count());
        System.out.print("openRaf_count -- ");
        System.out.println(res.openRaf_count());
        System.out.print("delivered_count -- ");
        System.out.println(res.delivered_count());
        System.out.print("non_delivered_count -- ");
        System.out.println(res.non_delivered_count());
        System.out.print("response_count -- ");
        System.out.println(res.response_count());
        System.out.print("non_response_count -- ");
        System.out.println(res.non_response_count());
        System.out.print("accepted_count -- ");
        System.out.println(res.accepted_count());
        System.out.print("rejected_count -- ");
        System.out.println(res.rejected_count());
        System.out.print("accepted_on_claimed_count -- ");
        System.out.println(res.accepted_on_claimed_count());
        System.out.print("accept_not_on_claimed_count -- ");
        System.out.println(res.accept_not_on_claimed_count());


        System.out.println("----------\n\n");
        Assert.assertTrue(categories.size() > 0);
        System.out.println("\t program performance Test -- SUCCESS");
    }


    // ################################################################
    private static Map<String, Object> makeConfig() {
        Map<String, Object> config = new HashMap<>();

        // IF NEEDED, CHANGE THESE VALUES TO STAGING SQL
        config.put(JdbcDS.JDBC_CONNECTIONURL, "jdbc:postgresql://cockroachdb-stg.apixio.com:26257/providers");
        config.put(JdbcDS.JDBC_DRIVERCLASSNAME, "org.postgresql.Driver");
        config.put(JdbcDS.JDBC_USERNAME, "root");
        config.put(JdbcDS.JDBC_PASSWORD, "root");

        config.put(JdbcDS.POOL_MAXTOTAL, 25);
        config.put(JdbcDS.SYS_VERBOSESQL, Boolean.TRUE);

        return config;
    }

}
