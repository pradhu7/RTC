package com.apixio.dao.apxdata;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.json.JSONObject;
import org.springframework.jdbc.core.RowMapper;

import com.apixio.XUUID;
import com.apixio.dao.apxdata.ApxDataDao.QueryKeys;
import com.apixio.datasource.springjdbc.Binding;
import com.apixio.datasource.springjdbc.DbField;
import com.apixio.datasource.springjdbc.DbTable;
import com.apixio.datasource.springjdbc.DbUtil;
import com.apixio.datasource.springjdbc.EntitiesDS;
import com.apixio.datasource.springjdbc.JdbcDS;
import com.apixio.datasource.springjdbc.JdbcTransaction;
import com.apixio.datasource.springjdbc.SqlStatement;
import com.apixio.restbase.config.MacroUtil;

/**
 */
public class RdbMetadataStore implements MetadataStore
{

    // tables managed here:
    // * apx_data - one row per rowkey in Cassandra instance ????
    // * apx_querykeys  - one row per querykey per row in apx_data

    /**
     * String constants for table and field names for centralization only.  Kind of a pain, really,
     * but allows easy changing of column names...
     */
    final static String FIELD_ID          = "id";
    final static String FIELD_PROTOCLASS  = "protoclass";
    final static String FIELD_GROUPINGID  = "grouping_id";
    final static String FIELD_PARTITIONID = "partition_id";
    final static String FIELD_DATACOUNT   = "data_count";
    final static String FIELD_TOTALSIZE   = "total_size";
    final static String FIELD_CREATEDAT   = "created_at";

    final static String FIELD_DATAID      = "data_id";
    final static String FIELD_QUERYKEY    = "query_key";
    final static String FIELD_INTVALUE    = "int_value";
    final static String FIELD_STRVALUE    = "str_value";
    final static String FIELD_BOOLVALUE   = "bool_value";

    final static String TABLE_DATA       = "apx_data";
    final static String TABLE_QUERYKEYS  = "apx_querykeys";

    private EnumSet<ApxMeta.Fields> onDups = EnumSet.of(
        ApxMeta.Fields.COUNT, ApxMeta.Fields.TOTAL_SIZE);

    /**
     * This is a totally hacky way to make it a bit easier to declare template SQL
     * statements that are evaluated only once while still allowing the easy ability
     * to change actual field names without search/replace on things.  Sigh... I
     * seem to make things more complicated than necessary sometimes.
     *
     * The idea is to use the macro system to substitute real/final RDB field names
     * into a SQL statement that uses the syntax like "select * from {THE_TABLE} ...".
     *
     * For this to work all table names and field names must be added as macros.
     */
    private final static Map<String,String> SQL_MACROS = initSqlMacros();
    private static Map<String,String> initSqlMacros()
    {
        Map<String,String> macros = new HashMap<>();

        // All field and table names should also be in this list

        macros.put("FIELD_ID",          FIELD_ID);
        macros.put("FIELD_PROTOCLASS",  FIELD_PROTOCLASS);
        macros.put("FIELD_GROUPINGID",  FIELD_GROUPINGID);
        macros.put("FIELD_PARTITIONID", FIELD_PARTITIONID);
        macros.put("FIELD_DATACOUNT",   FIELD_DATACOUNT);
        macros.put("FIELD_TOTALSIZE",   FIELD_TOTALSIZE);
        macros.put("FIELD_CREATED_AT",  FIELD_CREATEDAT);
        macros.put("FIELD_DATAID",      FIELD_DATAID);
        macros.put("FIELD_QUERYKEY",    FIELD_QUERYKEY);
        macros.put("FIELD_INTVALUE",    FIELD_INTVALUE);
        macros.put("FIELD_STRVALUE",    FIELD_STRVALUE);
        macros.put("FIELD_BOOLVALUE",   FIELD_BOOLVALUE);

        macros.put("TABLE_DATA",        TABLE_DATA);
        macros.put("TABLE_QUERYKEYS",   TABLE_QUERYKEYS);

        return macros;
    }

    /**
     * Substitute in the actual field names.  The String... args allows the use of
     * "%s" in the template SQL to allow substituting in other constants (e.g.,
     * enum string values).
     */
    private static String makeSql(String tpl, String... args)
    {
        String sql = MacroUtil.replaceMacros(SQL_MACROS, false, tpl);

        if (args.length > 0)
            sql = String.format(sql, args);

        return sql;
    }

    /**
     * Simple wrapper over lower-level non-nestable transactional code
     */
    private class TxWrapper
    {
        private boolean inTrans;

        TxWrapper()
        {
            inTrans = jdbcTx.inTransaction();

            if (!inTrans)
                jdbcTx.begin();
        }

        void commit()
        {
            if (!inTrans)
                jdbcTx.commit();
        }

        void abort()
        {
            if (!inTrans)
                jdbcTx.abort();
        }
    }

    /**
     * Fields
     */
    private JdbcDS              jdbcDS;
    private JdbcTransaction     jdbcTx;
    private EntitiesDS<ApxMeta> metaDS;

    /**
     * QK is a class used only internally to restore rows from the apx_querykeys table
     */
    private static class QK
    {
        int    dataID;
        String queryKey;

        // extensible area, one per type supported
        String  strValue;
        Boolean boolValue;
        Integer intValue;

        QK(int dataID, String queryKey, String strValue, Boolean boolValue, Integer intValue)
        {
            this.dataID   = dataID;
            this.queryKey = queryKey;

            this.strValue  = strValue;
            this.boolValue = boolValue;
            this.intValue  = intValue;
        }
    }

    /**
     * Construct a Meta instance for the db row
     */
    private static class QKRowMapper implements RowMapper<QK>
    {
        @Override
        public QK mapRow(ResultSet rs, int rowNum) throws SQLException
        {
            return new QK(
                rs.getInt(FIELD_DATAID),
                rs.getString(FIELD_QUERYKEY),

                rs.getString(FIELD_STRVALUE),
                rs.getBoolean(FIELD_BOOLVALUE),
                rs.getInt(FIELD_INTVALUE)
                );
        }
    }


    /**
     * select meta from groupingID
     */
    private final static String R_META_BY_GROUPING = makeSql(
        "select * from {TABLE_DATA} where {FIELD_GROUPINGID} = :{FIELD_GROUPINGID}");

    private final static String R_META_BY_GROUPING_PARTITION = makeSql(
        "select * from {TABLE_DATA} where {FIELD_GROUPINGID} = :{FIELD_GROUPINGID} AND {FIELD_PARTITIONID} = :{FIELD_PARTITIONID}");

    private final static String Q_GROUPINGS_BY_QK = makeSql("select distinct {FIELD_GROUPINGID} from {TABLE_DATA} ");

    // partial SQL for querying by querykeys; this produces just the subquery
    private final static String Q_DATAID_FROM_QK = "{FIELD_ID} in (select {FIELD_DATAID} from {TABLE_QUERYKEYS} where {FIELD_QUERYKEY} = :%s and %s = :%s)";

    /**
     * Deletion SQL
     */
    private final static String D_QK_BY_GROUPING = makeSql(
        "delete from {TABLE_QUERYKEYS} where {FIELD_DATAID} in" +
        " (select {FIELD_ID} from {TABLE_DATA} where {FIELD_GROUPINGID} = :{FIELD_GROUPINGID})");

    private final static String D_DATA_BY_GROUPING = makeSql(
        "delete from {TABLE_DATA} where {FIELD_GROUPINGID} = :{FIELD_GROUPINGID}");

    /**
     * DbTable takes care of some of the headache of forming SQL + binding info
     */
    private final static DbTable DBT_META = new DbTable<ApxMeta.Fields, ApxMeta>(
        TABLE_DATA,

        // these fields require transformation of the value returned from the class' getter
        DbField.viaExtractor(FIELD_CREATEDAT,  ApxMeta.Fields.CREATED_AT,  new DbField.Extractor<ApxMeta>() { public Object get(ApxMeta pojo) { return DbUtil.putTimestamp(pojo.getCreatedAt()); }}),

        // these fields don't require transformation and therefore can directly access class' getter
        DbField.viaMethodReference(FIELD_PROTOCLASS,  ApxMeta.Fields.PROTOCLASS,    ApxMeta::getProtoClass),
        DbField.viaMethodReference(FIELD_GROUPINGID,  ApxMeta.Fields.GROUPING_ID,   ApxMeta::getGroupingID),
        DbField.viaMethodReference(FIELD_PARTITIONID, ApxMeta.Fields.PARTITION_ID,  ApxMeta::getPartitionID),
        DbField.viaMethodReference(FIELD_DATACOUNT,   ApxMeta.Fields.COUNT,         ApxMeta::getCount),
        DbField.viaMethodReference(FIELD_TOTALSIZE,   ApxMeta.Fields.TOTAL_SIZE,    ApxMeta::getTotalSize)

        ).setIdField(DbField.viaMethodReference(FIELD_ID, ApxMeta.Fields.ID, ApxMeta::getDbID));

    /**
     * For reading in rows from TABLE_DATA
     */
    private static class MetaRowMapper implements RowMapper<ApxMeta>
    {
        @Override
        public ApxMeta mapRow(ResultSet rs, int rowNum) throws SQLException
        {
            return ApxMeta.fromPersisted(
                rs.getInt(FIELD_ID),
                rs.getString(FIELD_PROTOCLASS),
                rs.getString(FIELD_GROUPINGID),
                rs.getString(FIELD_PARTITIONID),
                DbUtil.getTimestamp(rs, FIELD_CREATEDAT, true),
                rs.getInt(FIELD_DATACOUNT),
                rs.getInt(FIELD_TOTALSIZE)
                );
        }
    }

    /**
     *
     */
    @Override
    public String getDataStoreID()
    {
        return ID;
    }

    @Override
    public void initialize(InitContext initContext)
    {
        if (initContext.phase == InitPhase.CREATE)
        {
            jdbcDS = initContext.daoServices.getJdbc("apxdatadao");
            jdbcTx = jdbcDS.getTransactionManager();

            metaDS = new EntitiesDS<ApxMeta>(jdbcDS, new MetaRowMapper(), null);
        }
    }

    /**
     * Creates a new row in apxdata table
     */
    @Override
    public void addOrUpdateMeta(ApxMeta meta, QueryKeys queryKeys)
    {
        TxWrapper tx = new TxWrapper();  // auto-starts transaction

        try
        {
            SqlStatement sql = DBT_META.createSqlUpsert(meta, onDups);

            meta.setDbID(metaDS.createEntity(sql.sql, sql.binding, true));

            if (meta.getDbID() == -1)  // row already existed (kinda ugly...) so reread to get dbid
                meta = getMeta(meta.getGroupingID(), meta.getPartitionID());

            if (queryKeys != null)
                saveQueryKeys(meta.getDbID(), queryKeys);

            tx.commit();
        }
        catch (Throwable t)
        {
            tx.abort();

            throw new RuntimeException("Failure to create ApxMeta", t);
        }
    }

    /**
     * Returns the list of ApxMetas whose groupingID matches the param.
     */
    @Override
    public List<ApxMeta> getMetasForGrouping(String groupingID)
    {
        Binding   bind = Binding.byName(FIELD_GROUPINGID, groupingID);

        return metaDS.loadEntities(R_META_BY_GROUPING, bind);
    }

    /**
     */
    private ApxMeta getMeta(String groupingID, String partitionID)
    {
        Binding   bind = Binding.byName(FIELD_GROUPINGID, groupingID).addByName(FIELD_PARTITIONID, partitionID);
        ApxMeta   meta;

        if (partitionID != null)
        {
            bind.addByName(FIELD_PARTITIONID, partitionID);
            meta = metaDS.loadEntity(R_META_BY_GROUPING_PARTITION, bind);
        }
        else
        {
            //            meta = metaDS.loadEntities(R_META_BY_GROUPING, bind);
            throw new RuntimeException("RdbMetadataStore.getMeta doesn't support null partitionIDs yet");
        }

        return meta;
    }

    /**
     * Selects the groupingIDs that match the given query key values.  And "and" is done
     * between all of the results of querying eacy querykey.
     */
    @Override
    public List<String> queryGroupingIDs(QueryKeys queryKeys)
    {
        StringBuilder where   = new StringBuilder();
        boolean       needAnd = false;
        Binding       binding = Binding.byName();
        int           bindNum = 0;

        // overall desired final SQL:
        //   select * from apx_data where
        //        id in (select data_id from apx_querykeys where query_key=:qk1 and str_value=:sv1)
        //    and id in (select data_id from apx_querykeys where query_key=:qk2 and str_value=:sv2)
        //    ...

        where.append("where ");

        for (Map.Entry<String,Object> entry : queryKeys.getKeys().entrySet())  // example entry:  pds_id -> o_123
        {
            // for sql field "query_key":
            String qkBind    = FIELD_QUERYKEY + "_" + bindNum;  // prepared statement var name #1
            String qkName    = entry.getKey();                  // value of prepared statement var name #1

            // for sql field {str,int,bool}_value:
            Object qvVal     = entry.getValue();                // value of prepared statement var name #2
            String fieldName = chooseFieldname(qvVal);          // str_value or int_value or bool_value
            String qvBind    = fieldName + "_" + bindNum;       // prepared statement var name #2

            // {FIELD_ID} in (select {FIELD_DATAID} from {TABLE_QUERYKEYS} where {FIELD_QUERYKEY} = :%s and %s = :%s)
            //  example makeSql(Q_DATAID_FROM_QK, "query_key_0", "str_value", "str_value_0")
            //  exxample binding:  {query_key_0="", str_value_0=""}

            binding.addByName(qkBind, qkName);
            binding.addByName(qvBind, qvVal);

            if (needAnd)
                where.append(" and ");

            where.append(makeSql(Q_DATAID_FROM_QK, qkBind, fieldName, qvBind));

            needAnd = true;
            bindNum++;
        }

        //        System.out.println("#### SFM queryGroupingIDs binding=" + binding);
        //        System.out.println("#### SFM queryGroupingIDs sql=" + Q_GROUPINGS_BY_QK + where.toString());

        return metaDS.getValues(Q_GROUPINGS_BY_QK + where.toString(), binding, String.class);
    }

    @Override
    public void deleteMetaByGrouping(String groupingID)
    {
        Binding binding = Binding.byName(FIELD_GROUPINGID, groupingID);

        TxWrapper tx = new TxWrapper();  // auto-starts transaction

        try
        {
            metaDS.deleteEntities(D_QK_BY_GROUPING, binding);
            metaDS.deleteEntities(D_DATA_BY_GROUPING, binding);

            tx.commit();
        }
        catch (Throwable t)
        {
            tx.abort();

            throw new RuntimeException("Failure to create ApxMeta", t);
        }
    }

    /**
     * This does a replace of the values if the unique index (data_id, query_key) exists
     * already.
     */
    private void saveQueryKeys(int dbID, QueryKeys qks)
    {
        for (Map.Entry<String, Object> entry : qks.getKeys().entrySet())
        {
            Binding   binding = Binding.byName(FIELD_DATAID, dbID);  // recreate binding each time otherwise we accumulate values by name
            Object    val     = entry.getValue();
            String    field   = chooseFieldname(val);

            binding.addByName(FIELD_QUERYKEY, entry.getKey());
            binding.addByName(field,          val);

            metaDS.createEntity(makeSql("insert into {TABLE_QUERYKEYS} " +
                                        "({FIELD_DATAID}, {FIELD_QUERYKEY}, %s) values (:{FIELD_DATAID}, :{FIELD_QUERYKEY}, :%s)" +
                                        " on duplicate key update %s = :%s",
                                        field, field,
                                        field, field  // for the "update %s = :%s" part
                                    ),
                                binding);
        }
    }
    
    private String chooseFieldname(Object val)
    {
        if (val instanceof String)
            return FIELD_STRVALUE;
        else if (val instanceof Integer)
            return FIELD_INTVALUE;
        else if (val instanceof Boolean)
            return FIELD_BOOLVALUE;
        else
            throw new IllegalArgumentException("Unsupported value type: " + val);
    }



    // ################################################################

    /*
    public static void main(String... args)
    {
        Blob         b   = Blob.newBlob("scott", null, makeJson(), makeJson());
        SqlStatement sql = DBT_BLOBS.createSqlInsert(b);

        System.out.println("\n################ insert");
        System.out.println(sql.sql);
        System.out.println(sql.binding);

        b.setSoftDeleted(true);
        sql = DBT_BLOBS.createSqlUpdate(b, b.getModified());
        System.out.println("\n################ update");
        System.out.println(sql.sql);
        System.out.println(sql.binding);
    }

    private static JSONObject makeJson()
    {
        JSONObject jo = new JSONObject();

        jo.put("field", "value");

        return jo;
    }
    */

}
