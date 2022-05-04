package com.apixio.dao.provider;

import com.apixio.dao.utility.sql.ExecutableOffsetQuery;
import com.apixio.dao.utility.sql.SqlDaoUtility;
import com.apixio.dao.utility.sql.SqlOffsetIterator;
import com.apixio.dao.utility.sql.SqlOrdering;
import com.apixio.daoquery.ProgramPerformanceQueryHelper;
import com.apixio.datasource.springjdbc.Binding;
import com.apixio.datasource.springjdbc.EntitiesDS;
import com.apixio.datasource.springjdbc.JdbcDS;
import com.apixio.model.prospective.DistinctProviders;
import com.apixio.model.prospective.ProgramPerformance;
import com.apixio.model.prospective.ProviderPerformance;
import com.apixio.model.prospective.ProviderResponse;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This DAO is used to interface with the Mappings DB
 * PVCA document for provider performance dashboard.. https://apixio.atlassian.net/wiki/spaces/PM/pages/1298366479/Dashboards
 */
public class ProviderDAO {

    private EntitiesDS<ProviderResponse> providerResponseDS; //Support serialization / deserializtion of ProviderResponse
    private EntitiesDS<ProviderPerformance> providerPerformanceDS;
    private EntitiesDS<DistinctProviders> providerDS; //Support for retrieving list of npi, provider names and groups
    private EntitiesDS<ValueAndIsClaimedCounts> countAndGroupAndBoolDS; //// Support for getting counts grouped by isClaimed and secondary field
    private EntitiesDS<ProgramPerformance> programPerformanceDS;

    // table provider_opps
    private final static String TABLE_PROVIDER_OPPS = "provider_opps";
    private final static String FIELD_PROJECT_ID = "projId";            // string
    private final static String FIELD_CODE = "code";              // string
    private final static String FIELD_PATIENT_ID = "patId";             // string
    private final static String FIELD_PDS_ID = "pdsId";             // string
    private final static String FIELD_RAF = "raf";               // double
    private final static String FIELD_DISEASE_CATEGORY = "diseaseCategory";   // string
    private final static String FIELD_DECISION = "decision";          // string
    private final static String FIELD_NPI = "npi";               // string
    private final static String FIELD_PROVIDER_GROUP = "providerGroup";     // string
    private final static String FIELD_PROVIDER_NAME = "providerName";      // string
    private final static String FIELD_IS_CLAIMED = "isClaimed";         // boolean
    private final static String FIELD_REPORTABLE_RAF = "reportableRaf";       // double
    private final static String FIELD_HIERARCHY_FILTERED = "hierarchyFiltered"; // boolean
    private final static String FIELD_DELIVERY_DATE = "deliveryDate";      // long nullable
    private final static String FIELD_RESPONSE_DATE = "responseDate";      // long nullable
    private final static String FIELD_LASTMODIFIED_DATE = "lastModifiedDate"; //long, should be non nullable

    //Fields used for complex provider performance query
    private final static String FIELD_TOTAL_COUNT           = "total";
    private final static String FIELD_CLAIMED_COUNT         = "claimed";
    private final static String FIELD_CLAIMED_RAF_COUNT     = "claimRaf";
    private final static String FIELD_OPEN_RAF_COUNT        = "openRaf";
    private final static String FIELD_DELIVERED_COUNT       = "delivered";
    private final static String FIELD_NOT_DELIVERED_COUNT   = "notDelivered";
    private final static String FIELD_ACCEPTED_COUNT        = "accepted";
    private final static String FIELD_REJECTED_COUNT        = "rejected";
    private final static String FIELD_SNOOZED_COUNT         = "snoozed";
    private final static String FIELD_ACCEPT_ON_CLAIMED_COUNT = "accepted_on_claimed_count";
    private final static String FIELD_RESPONSE_COUNT = "responseCount";
    private final static String FIELD_ACCEPT_RATE = "acceptRate";
    private final static String FIELD_RESPONSE_RATE = "responseRate";

    public void init(JdbcDS jdbc) {
        this.providerResponseDS = new EntitiesDS<>(jdbc, new ProviderResponseRowMapper(), null);
        this.providerPerformanceDS = new EntitiesDS<>(jdbc, new ProviderPerformanceRowMapper(), null);
        this.providerDS = new EntitiesDS<>(jdbc, new ProviderRowMapper(), null);
        this.countAndGroupAndBoolDS = new EntitiesDS<>(jdbc, new ValueAndIsClaimedCounts.CountAndBooleanRowMapper());
        this.programPerformanceDS = new EntitiesDS<>(jdbc, new ProgramPerformanceRowMapper());
    }

    private final static Map<String, String> SQL_MACROS = initSqlMacros();

    private static Map<String, String> initSqlMacros() {
        Map<String, String> macros = new HashMap<>();
        // All field and table names should be in this list
        macros.put("TABLE_PROVIDER_OPPS", TABLE_PROVIDER_OPPS);

        macros.put("FIELD_PROJECT_ID", FIELD_PROJECT_ID);
        macros.put("FIELD_CODE", FIELD_CODE);
        macros.put("FIELD_PATIENT_ID", FIELD_PATIENT_ID);
        macros.put("FIELD_PDS_ID", FIELD_PDS_ID);
        macros.put("FIELD_RAF", FIELD_RAF);
        macros.put("FIELD_DISEASE_CATEGORY", FIELD_DISEASE_CATEGORY);
        macros.put("FIELD_DECISION", FIELD_DECISION);
        macros.put("FIELD_NPI", FIELD_NPI);
        macros.put("FIELD_PROVIDER_GROUP", FIELD_PROVIDER_GROUP);
        macros.put("FIELD_PROVIDER_NAME", FIELD_PROVIDER_NAME);
        macros.put("FIELD_IS_CLAIMED", FIELD_IS_CLAIMED);
        macros.put("FIELD_REPORTABLE_RAF", FIELD_REPORTABLE_RAF);
        macros.put("FIELD_HIERARCHY_FILTERED", FIELD_HIERARCHY_FILTERED);
        macros.put("FIELD_DELIVERY_DATE", FIELD_DELIVERY_DATE);
        macros.put("FIELD_RESPONSE_DATE", FIELD_RESPONSE_DATE);
        macros.put("FIELD_LASTMODIFIED_DATE", FIELD_LASTMODIFIED_DATE);

        // For Group and IsClaimed Counts
        macros.put("FIELD_COUNT", ValueAndIsClaimedCounts.FIELD_COUNT);
        macros.put("FIELD_VALUE", ValueAndIsClaimedCounts.FIELD_VALUE);
        macros.put("FIELD_BOOL", ValueAndIsClaimedCounts.FIELD_BOOL);

        // For Provider/Program Performance
        macros.put("FIELD_TOTAL_COUNT", FIELD_TOTAL_COUNT);
        macros.put("FIELD_CLAIMED_COUNT", FIELD_CLAIMED_COUNT);
        macros.put("FIELD_CLAIMED_RAF_COUNT", FIELD_CLAIMED_RAF_COUNT);
        macros.put("FIELD_OPEN_RAF_COUNT", FIELD_OPEN_RAF_COUNT);
        macros.put("FIELD_DELIVERED_COUNT", FIELD_DELIVERED_COUNT);
        macros.put("FIELD_NOT_DELIVERED_COUNT", FIELD_NOT_DELIVERED_COUNT);
        macros.put("FIELD_ACCEPTED_COUNT", FIELD_ACCEPTED_COUNT);
        macros.put("FIELD_REJECTED_COUNT", FIELD_REJECTED_COUNT);
        macros.put("FIELD_SNOOZED_COUNT", FIELD_SNOOZED_COUNT);
        macros.put("FIELD_ACCEPT_ON_CLAIMED_COUNT", FIELD_ACCEPT_ON_CLAIMED_COUNT);

        macros.put("FIELD_RESPONSE_COUNT", FIELD_RESPONSE_COUNT);
        macros.put("FIELD_ACCEPT_RATE", FIELD_ACCEPT_RATE);
        macros.put("FIELD_RESPONSE_RATE", FIELD_RESPONSE_RATE);
        return macros;
    }

    /**
     * For reading in rows from TABLE_PROVIDER_OPPS
     */
    private static class ProgramPerformanceRowMapper implements RowMapper<ProgramPerformance> {
        @Override
        public ProgramPerformance mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new ProgramPerformance(rs.getLong(FIELD_TOTAL_COUNT),
                    rs.getLong(FIELD_CLAIMED_COUNT),
                    rs.getFloat(FIELD_CLAIMED_RAF_COUNT),
                    rs.getFloat(FIELD_OPEN_RAF_COUNT),
                    rs.getLong(FIELD_DELIVERED_COUNT),
                    rs.getLong(FIELD_ACCEPTED_COUNT),
                    rs.getLong(FIELD_REJECTED_COUNT),
                    rs.getLong(FIELD_SNOOZED_COUNT),
                    rs.getLong(FIELD_ACCEPT_ON_CLAIMED_COUNT));
        }
    }

    private static class ProviderResponseRowMapper implements RowMapper<ProviderResponse> {
        @Override
        public ProviderResponse mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new ProviderResponse(rs.getString(FIELD_PROJECT_ID),
                    rs.getString(FIELD_CODE),
                    rs.getString(FIELD_PATIENT_ID),
                    rs.getString(FIELD_PDS_ID),
                    rs.getFloat(FIELD_RAF),
                    rs.getString(FIELD_DISEASE_CATEGORY),
                    rs.getString(FIELD_DECISION),
                    rs.getString(FIELD_NPI),
                    rs.getString(FIELD_PROVIDER_GROUP),
                    rs.getString(FIELD_PROVIDER_NAME),
                    rs.getBoolean(FIELD_IS_CLAIMED),
                    rs.getFloat(FIELD_REPORTABLE_RAF),
                    rs.getBoolean(FIELD_HIERARCHY_FILTERED),
                    rs.getLong(FIELD_DELIVERY_DATE),
                    rs.getLong(FIELD_RESPONSE_DATE),
                    rs.getLong(FIELD_LASTMODIFIED_DATE));
        }
    }

    /**
     * For reading in rows from TABLE_PROVIDER_OPPS
     */
    private static class ProviderPerformanceRowMapper implements RowMapper<ProviderPerformance> {
        @Override
        public ProviderPerformance mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new ProviderPerformance(
                    rs.getString(FIELD_PROJECT_ID),
                    rs.getString(FIELD_PROVIDER_NAME),
                    rs.getString(FIELD_PROVIDER_GROUP),
                    rs.getString(FIELD_NPI),
                    rs.getLong(FIELD_TOTAL_COUNT),
                    rs.getLong(FIELD_DELIVERED_COUNT),
                    rs.getLong(FIELD_NOT_DELIVERED_COUNT),
                    rs.getLong(FIELD_ACCEPTED_COUNT),
                    rs.getLong(FIELD_REJECTED_COUNT),
                    rs.getLong(FIELD_SNOOZED_COUNT),
                    rs.getLong(FIELD_CLAIMED_COUNT),
                    rs.getFloat(FIELD_OPEN_RAF_COUNT)
            );
        }
    }

    private static class ProviderRowMapper implements RowMapper<DistinctProviders> {
        @Override
        public DistinctProviders mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new DistinctProviders(
                rs.getString(FIELD_NPI),
                rs.getString(FIELD_PROVIDER_NAME),
                rs.getString(FIELD_PROVIDER_GROUP));
        }
    }

    private Map<String, Object> unmap(ProviderResponse pv) {
        Map<String, Object> fields = new HashMap<>();

        fields.put(FIELD_PROJECT_ID, pv.projectId());
        fields.put(FIELD_CODE, pv.code());
        fields.put(FIELD_PATIENT_ID, pv.patientId());
        fields.put(FIELD_PDS_ID, pv.pds());
        fields.put(FIELD_RAF, pv.raf());
        fields.put(FIELD_DISEASE_CATEGORY, pv.diseaseCategory());
        fields.put(FIELD_DECISION, pv.decision());
        fields.put(FIELD_NPI, pv.npi());
        fields.put(FIELD_PROVIDER_GROUP, pv.providerGroup());
        fields.put(FIELD_PROVIDER_NAME, pv.providerName());
        fields.put(FIELD_IS_CLAIMED, pv.isClaimed());
        fields.put(FIELD_REPORTABLE_RAF, pv.reportableRaf());
        fields.put(FIELD_HIERARCHY_FILTERED,  pv.hierarchyFiltered());
        fields.put(FIELD_DELIVERY_DATE, pv.deliveryDate());
        fields.put(FIELD_RESPONSE_DATE, pv.responseDate());
        fields.put(FIELD_LASTMODIFIED_DATE, pv.lastModifiedDate());

        return fields;
    }

    private static String makeSql(String tpl, String... args) {
        return SqlDaoUtility.makeSql(SQL_MACROS, tpl, args);
    }

    private final static String RI_PROVIDER_RESPONSE = makeSql(
            "UPSERT INTO {TABLE_PROVIDER_OPPS} " +
                    "({FIELD_PROJECT_ID}, {FIELD_CODE}, {FIELD_PATIENT_ID}, {FIELD_PDS_ID}, {FIELD_RAF}, {FIELD_DISEASE_CATEGORY}, " +
                    "{FIELD_DECISION}, {FIELD_NPI}, {FIELD_PROVIDER_GROUP}, {FIELD_PROVIDER_NAME}, {FIELD_IS_CLAIMED}, " +
                    "{FIELD_REPORTABLE_RAF}, {FIELD_HIERARCHY_FILTERED}, {FIELD_DELIVERY_DATE}, {FIELD_RESPONSE_DATE}, {FIELD_LASTMODIFIED_DATE})" +
                    "values (:{FIELD_PROJECT_ID}, :{FIELD_CODE}, :{FIELD_PATIENT_ID}, :{FIELD_PDS_ID}, :{FIELD_RAF}, " +
                    ":{FIELD_DISEASE_CATEGORY}, :{FIELD_DECISION}, :{FIELD_NPI}, :{FIELD_PROVIDER_GROUP}, " +
                    ":{FIELD_PROVIDER_NAME}, :{FIELD_IS_CLAIMED}, :{FIELD_REPORTABLE_RAF}, :{FIELD_HIERARCHY_FILTERED}, " +
                    ":{FIELD_DELIVERY_DATE}, :{FIELD_RESPONSE_DATE}, :{FIELD_LASTMODIFIED_DATE})");

    private final static String U_PROVIDER_RESPONSE = makeSql(
            "update {TABLE_PROVIDER_OPPS} SET " +
                    "{FIELD_DECISION} = :{FIELD_DECISION}, " +
                    "{FIELD_NPI} = :{FIELD_NPI}, " +
                    "{FIELD_PROVIDER_GROUP} = :{FIELD_PROVIDER_GROUP}, " +
                    "{FIELD_PROVIDER_NAME} = :{FIELD_PROVIDER_NAME}, " +
                    "{FIELD_IS_CLAIMED} = :{FIELD_IS_CLAIMED}, " +
                    "{FIELD_REPORTABLE_RAF} = :{FIELD_REPORTABLE_RAF}, " +
                    "{FIELD_HIERARCHY_FILTERED} = :{FIELD_HIERARCHY_FILTERED}, " +
                    "{FIELD_DELIVERY_DATE} = :{FIELD_DELIVERY_DATE}, " +
                    "{FIELD_RESPONSE_DATE} = :{FIELD_RESPONSE_DATE}, " +
                    "{FIELD_LASTMODIFIED_DATE} = :{FIELD_LASTMODIFIED_DATE} " +
                    "WHERE {FIELD_PROJECT_ID} = :{FIELD_PROJECT_ID} " +
                    "AND {FIELD_CODE} = :{FIELD_CODE} " +
                    "AND {FIELD_PATIENT_ID} = :{FIELD_PATIENT_ID}"
    );

    private final static String D_PROVIDER_RESPONSE = makeSql(
            "DELETE FROM {TABLE_PROVIDER_OPPS} " +
                    "WHERE {FIELD_PROJECT_ID} = :{FIELD_PROJECT_ID} " +
                    "AND {FIELD_CODE} = :{FIELD_CODE} " +
                    "AND {FIELD_PATIENT_ID} = :{FIELD_PATIENT_ID}"
    );


    private static Binding projectBinding(String projectId) {
        return Binding.byName(FIELD_PROJECT_ID, projectId);
    }

    private static Binding primaryKeyBinding(String projectId, String code, String patientId) {
        return Binding.byName(FIELD_PROJECT_ID, projectId)
                .addByName(FIELD_CODE, code)
                .addByName(FIELD_PATIENT_ID, patientId);
    }

    private static String providersFilter(List<String> providerNames, List<String> providerGroups) {
        return providersFilter(providerNames, providerGroups, true);
    }

    private static String providersFilter(List<String> providerNames, List<String> providerGroups, boolean useAnd) {
        if (providerNames.isEmpty() && providerGroups.isEmpty()) return "";
        else {
            String prefix = useAnd ? "AND " : "WHERE";
            String nameFilter = SqlDaoUtility.strList(providerNames);
            String groupFilter = SqlDaoUtility.strList(providerGroups);
            String clause;
            if (providerNames.isEmpty()) {
                clause = String.format("{FIELD_PROVIDER_GROUP} in (%s)", groupFilter);
            } else if (providerGroups.isEmpty()) {
                clause = String.format("{FIELD_PROVIDER_NAME} in (%s)", nameFilter);
            } else {
                clause = String.format("{FIELD_PROVIDER_NAME} in (%s) AND {FIELD_PROVIDER_GROUP} in (%s)", nameFilter, groupFilter);
            }
            return makeSql(String.format("%s %s", prefix, clause));
        }
    }

    ///////////////////////////// Write path ////////////////////////////////////
    public void insertProviderResponses(List<ProviderResponse> rows) {
        for (ProviderResponse pv : rows) {
            providerResponseDS.createEntity(RI_PROVIDER_RESPONSE, Binding.byName(unmap(pv)));
        }
    }

    //Update the relevant rows. Used when the primary key already exists
    public void updateProviderResponses(List<ProviderResponse> rows) {
        for (ProviderResponse pv : rows) {
            providerResponseDS.updateEntities(U_PROVIDER_RESPONSE, Binding.byName(unmap(pv)));
        }
    }


    //Delete Row
    public void deleteProviderResponse(ProviderResponse pv) {
        providerResponseDS.deleteEntities(D_PROVIDER_RESPONSE,
                primaryKeyBinding(pv.projectId(), pv.code(), pv.patientId()));
    }


    public List<ProgramPerformance> getProgramPerformance(String projectId, List<String> providerNames, List<String> providerGroups) {
        String pFilter = providersFilter(providerNames, providerGroups);
        String query = makeSql("SELECT " +
                ProgramPerformanceQueryHelper.TotalCountQ() +
                ProgramPerformanceQueryHelper.ClaimedCountQ() +
                ProgramPerformanceQueryHelper.ClaimedRafQ()+
                ProgramPerformanceQueryHelper.OpendRafQ() +
                ProgramPerformanceQueryHelper.DeliveredCountQ() +
                ProgramPerformanceQueryHelper.AcceptedCountQ() +
                ProgramPerformanceQueryHelper.RejectedCountQ() +
                ProgramPerformanceQueryHelper.SnoozedCountQ() +
                ProgramPerformanceQueryHelper.AcceptedOnClaimedCountQ(false) +
                "FROM {TABLE_PROVIDER_OPPS} " +
                "WHERE {FIELD_PROJECT_ID} = :{FIELD_PROJECT_ID}" +
                " %s", pFilter);
        return programPerformanceDS.loadEntities(query, projectBinding(projectId));
    }


    ////////////////////////////// Provider Performance /////////////////////////

    public SqlOffsetIterator<ProviderPerformance> providerPerformanceIterator(String projectId,
                                                                              List<String> providerNames,
                                                                              List<String> providerGroups,
                                                                              List<SqlOrdering> sortFields,
                                                                              int pagination,
                                                                              int startPage) {
        String pFilter = providersFilter(providerNames, providerGroups);
        String query = makeSql("SELECT" +
                " max({FIELD_PROVIDER_NAME}) as {FIELD_PROVIDER_NAME}," + // They should all be the same
                " max({FIELD_PROVIDER_GROUP}) as {FIELD_PROVIDER_GROUP}," + // They should all be the same
                " :{FIELD_PROJECT_ID} as {FIELD_PROJECT_ID}," +
                " {FIELD_NPI}," +
                " count(*) as {FIELD_TOTAL_COUNT}," +
                ProgramPerformanceQueryHelper.DeliveredCountQ() +
                ProgramPerformanceQueryHelper.NotDeliveredCountQ() +
                ProgramPerformanceQueryHelper.OpendRafQ() +
                ProgramPerformanceQueryHelper.AcceptedCountQ() +
                ProgramPerformanceQueryHelper.RejectedCountQ() +
                ProgramPerformanceQueryHelper.SnoozedCountQ() +
                ProgramPerformanceQueryHelper.ClaimedCountQ() +
                ProgramPerformanceQueryHelper.responseCountQ() +
                ProgramPerformanceQueryHelper.responseRateQ() +
                ProgramPerformanceQueryHelper.acceptRateQ(false) +
                " FROM {TABLE_PROVIDER_OPPS} " +
                " WHERE {FIELD_PROJECT_ID} = :{FIELD_PROJECT_ID}" +
                " %s"+
                " GROUP BY {FIELD_NPI} " +
                SqlDaoUtility.orderBy(sortFields), pFilter);
        Binding binding = projectBinding(projectId);
        ExecutableOffsetQuery<ProviderPerformance> providerResponseExecutableQuery =
                new ExecutableOffsetQuery<>(query, providerPerformanceDS, binding);

        return new SqlOffsetIterator<>(pagination, startPage, providerResponseExecutableQuery);
    }


    ///////////////////////////// Provider APIs ////////////////////////
    public SqlOffsetIterator<ProviderResponse> getProviderResponsesIterator(String projectId,
                                                                            List<String> providerNames,
                                                                            List<String> providerGroups,
                                                                            List<SqlOrdering> sortFields,
                                                                            int pagination,
                                                                            int startPage) {
        String pFilter = providersFilter(providerNames, providerGroups);
        String query = makeSql("SELECT * FROM {TABLE_PROVIDER_OPPS} where {FIELD_PROJECT_ID} = :{FIELD_PROJECT_ID} " +
                " %s" +
                SqlDaoUtility.orderBy(sortFields), pFilter);
        Binding binding = projectBinding(projectId);
        ExecutableOffsetQuery<ProviderResponse> providerResponseExecutableQuery =
                new ExecutableOffsetQuery<>(query, providerResponseDS, binding);

        return new SqlOffsetIterator<>(pagination, startPage, providerResponseExecutableQuery);
    }

    public List<ProviderResponse> getNPIResponses(String projectId, List<String> npis) {
        String providerList = SqlDaoUtility.strList(npis);
        return providerResponseDS.loadEntities(
                makeSql("SELECT * FROM {TABLE_PROVIDER_OPPS}" +
                        " WHERE {FIELD_PROJECT_ID} = :{FIELD_PROJECT_ID}" +
                        " AND {FIELD_NPI} in (%s)", providerList),
                Binding.byName(FIELD_PROJECT_ID, projectId));
    }

    public SqlOffsetIterator<DistinctProviders> getProvidersIterator(String projectId, int pagination, int startPage) {
        String query = makeSql("SELECT DISTINCT {FIELD_NPI}, {FIELD_PROVIDER_NAME}, {FIELD_PROVIDER_GROUP} FROM {TABLE_PROVIDER_OPPS}" +
                " WHERE {FIELD_PROJECT_ID} = :{FIELD_PROJECT_ID}");
        Binding binding = projectBinding(projectId);
        ExecutableOffsetQuery<DistinctProviders> provideExecutableQuery =
                new ExecutableOffsetQuery<>(query, providerDS, binding);

        return new SqlOffsetIterator<>(pagination, startPage, provideExecutableQuery);
    }


    /////////////////////////// Program Performance ////////////////////////////
    public Long totalOppsCount(String projectId, List<String> providerNames, List<String> providerGroups) {
        String pFilter = providersFilter(providerNames, providerGroups);
        return providerResponseDS.getValue(makeSql("SELECT count(*) FROM {TABLE_PROVIDER_OPPS}" +
                        " WHERE {FIELD_PROJECT_ID} = :{FIELD_PROJECT_ID}" +
                        " %s", pFilter),
                projectBinding(projectId),
                Long.class);
    }

    public Long totalPatientCount(String projectId, List<String> providerNames, List<String> providerGroups) {
        String pFilter = providersFilter(providerNames, providerGroups);
        return providerResponseDS.getValue(makeSql("SELECT count(distinct({FIELD_PATIENT_ID})) FROM {TABLE_PROVIDER_OPPS}" +
                        " WHERE {FIELD_PROJECT_ID} = :{FIELD_PROJECT_ID}" +
                        " %s", pFilter),
                projectBinding(projectId),
                Long.class);
    }

    public List<ValueAndIsClaimedCounts> diseaseCategoryByClaimed(String projectId, List<String> providerNames, List<String> providerGroups) {
        String pFilter = providersFilter(providerNames, providerGroups);
        return countAndGroupAndBoolDS.loadEntities(makeSql("SELECT count(*) as {FIELD_COUNT}," +
                        " {FIELD_DISEASE_CATEGORY} as {FIELD_VALUE}," +
                        " {FIELD_IS_CLAIMED} as {FIELD_BOOL}" +
                        " FROM {TABLE_PROVIDER_OPPS}" +
                        " WHERE {FIELD_PROJECT_ID} = :{FIELD_PROJECT_ID}" +
                        " %s" +
                        " GROUP BY {FIELD_VALUE}, {FIELD_BOOL}", pFilter),
                projectBinding(projectId));
    }

    public List<ValueAndIsClaimedCounts> deliveredByClaimed(String projectId, List<String> providerNames, List<String> providerGroups) {
        String pFilter = providersFilter(providerNames, providerGroups);
        return countAndGroupAndBoolDS.loadEntities(makeSql("SELECT count(*) as {FIELD_COUNT}," +
                        " CASE WHEN {FIELD_DELIVERY_DATE} > 0 THEN 'not_delivered' ELSE 'delivered' END as {FIELD_VALUE}," +
                        " {FIELD_IS_CLAIMED} as {FIELD_BOOL}" +
                        " FROM {TABLE_PROVIDER_OPPS}" +
                        " WHERE {FIELD_PROJECT_ID} = :{FIELD_PROJECT_ID}" +
                        " %s" +
                        " GROUP BY {FIELD_VALUE}, {FIELD_BOOL}", pFilter),
                projectBinding(projectId));
    }

    public List<ValueAndIsClaimedCounts> respondedByClaimed(String projectId,
                                                            List<String> providerNames,
                                                            List<String> providerGroups,
                                                            boolean requireDelivery) {
        String pFilter = providersFilter(providerNames, providerGroups);
        String deliveryFilter = requireDelivery ? makeSql("AND {FIELD_DELIVERY_DATE} > 0") : "";
        return countAndGroupAndBoolDS.loadEntities(makeSql("SELECT count(*) as {FIELD_COUNT}," +
                        " CASE WHEN {FIELD_RESPONSE_DATE} = 0 THEN 'no_response' ELSE 'has_response' END as {FIELD_VALUE}," +
                        " {FIELD_IS_CLAIMED} as {FIELD_BOOL}" +
                        " FROM {TABLE_PROVIDER_OPPS}" +
                        " WHERE {FIELD_PROJECT_ID} = :{FIELD_PROJECT_ID}" +
                        " AND {FIELD_DELIVERY_DATE} > 0 " +
                        " %s" +
                        " %s" +
                        " GROUP BY {FIELD_VALUE}, {FIELD_BOOL}", pFilter, deliveryFilter),
                projectBinding(projectId));
    }


    public List<ValueAndIsClaimedCounts> decisionsByClaimed(String projectId,
                                                            List<String> providerNames,
                                                            List<String> providerGroups,
                                                            boolean requireResponse) {
        String pFilter = providersFilter(providerNames, providerGroups);
        String responseFilter = requireResponse ? makeSql("AND {FIELD_RESPONSE_DATE} > 0") : "";
        return countAndGroupAndBoolDS.loadEntities(makeSql("SELECT count(*) as {FIELD_COUNT}," +
                        " {FIELD_DECISION} as {FIELD_VALUE}," +
                        " {FIELD_IS_CLAIMED} as {FIELD_BOOL}" +
                        " FROM {TABLE_PROVIDER_OPPS}" +
                        " WHERE {FIELD_PROJECT_ID} = :{FIELD_PROJECT_ID}" +
                        " AND {FIELD_RESPONSE_DATE} > 0 " +
                        " %s" +
                        " %s" +
                        " GROUP BY {FIELD_VALUE}, {FIELD_BOOL}", pFilter, responseFilter),
                projectBinding(projectId));
    }

    public List<ValueAndIsClaimedCounts> rafByClaimedAndDecision(String projectId, List<String> providerNames, List<String> providerGroups) {
        String pFilter = providersFilter(providerNames, providerGroups);
        return countAndGroupAndBoolDS.loadEntities(makeSql("SELECT sum({FIELD_RAF}) as {FIELD_COUNT}," +
                        " {FIELD_DECISION} as {FIELD_VALUE}," +
                        " {FIELD_IS_CLAIMED} as {FIELD_BOOL}" +
                        " FROM {TABLE_PROVIDER_OPPS}" +
                        " WHERE {FIELD_PROJECT_ID} = :{FIELD_PROJECT_ID}" +
                        " %s" +
                        " GROUP BY {FIELD_VALUE}, {FIELD_BOOL}", pFilter),
                projectBinding(projectId));
    }

    public List<ValueAndIsClaimedCounts> rafByClaimedAndReportable(String projectId, List<String> providerNames, List<String> providerGroups) {
        String pFilter = providersFilter(providerNames, providerGroups);
        return countAndGroupAndBoolDS.loadEntities(makeSql("SELECT sum({FIELD_REPORTABLE_RAF}) as {FIELD_COUNT}," +
                        " {FIELD_HIERARCHY_FILTERED} as {FIELD_VALUE}," +
                        " {FIELD_IS_CLAIMED} as {FIELD_BOOL}" +
                        " FROM {TABLE_PROVIDER_OPPS}" +
                        " WHERE {FIELD_PROJECT_ID} = :{FIELD_PROJECT_ID}" +
                        " AND {FIELD_DECISION} != 'rejected' "+
                        " %s" +
                        " GROUP BY {FIELD_VALUE}, {FIELD_BOOL}", pFilter),
                projectBinding(projectId));
    }
}