package com.apixio.bms;

import com.apixio.XUUID;
import com.apixio.datasource.springjdbc.Binding;
import com.apixio.datasource.springjdbc.DbField;
import com.apixio.datasource.springjdbc.DbTable;
import com.apixio.datasource.springjdbc.DbUtil;
import com.apixio.datasource.springjdbc.EntitiesDS;
import com.apixio.datasource.springjdbc.JdbcDS;
import com.apixio.datasource.springjdbc.JdbcTransaction;
import com.apixio.datasource.springjdbc.SqlStatement;
import com.apixio.restbase.config.MacroUtil;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.json.JSONObject;
import org.springframework.jdbc.core.RowMapper;

/**
 * Implementation of MetaStorage that uses MariaDB.  The big reason for this tie to
 * MariaDB (and version 10.2.4+ specifically) is that the query mechanism on the
 * client-defined JSON "extra" data is specific to the RDBMS system (e.g., MySQL
 * supports JSON in an entirely different way).
 */
public class MariaDbStorage implements MetaStorage
{
    /**
     * The Blob relationship model:
     *
     *  1) (unique) logicalIDs are owned by (XUUID) phsyicalIDs; a physicalID is the XUUID
     *     of a Blob
     *
     *  2) a physicalID owning a logicalID means that some portion of the s3 file(s)
     *     associated with the physicalID comprise the bits of the logicalID.  E.g., a
     *     single signal generator with a logicalID of, say, "f2f:1.1.0" would have its
     *     .class files in one of the files in the owning MCID
     *
     *  3) logicalIDs (and *not* phyical Blob IDs) can depend on (or "use") logicalIDs.
     *
     * "Owns" is a synonym for "provides".
     */
    public enum RelationType
    {
        OWNS,   // a physicalID owns zero or more logicalIDs
        USES;   // a logicalID uses/depends-on zero or more logicalIDs
    }

    // tables managed here:
    // * blobmgr_blobs - one row per Blob instance
    // * blobmgr_meta  - one row per metadata field per Blob instance

    /**
     * String constants for table and field names for centralization only.  Kind of a pain, really,
     * but allows easy changing of column names...
     */
    final static String FIELD_BLOB_ID     = "blob_id";
    final static String FIELD_BLOB_PARENT = "blob_parent";
    final static String FIELD_BLOB_UUID   = "blob_uuid";
    final static String FIELD_BOOL_VAL    = "bool_val";
    final static String FIELD_CREATED_AT  = "created_at";
    final static String FIELD_CREATED_BY  = "created_by";
    final static String FIELD_DBL_VAL     = "dbl_val";
    final static String FIELD_EXTRA1_META = "extra_1";
    final static String FIELD_EXTRA2_META = "extra_2";
    final static String FIELD_INT_VAL     = "int_val";
    final static String FIELD_IS_DELETED  = "is_deleted";
    final static String FIELD_IS_LOCKED   = "is_locked";
    final static String FIELD_KEY_NAME    = "key_name";
    final static String FIELD_MD5_DIGEST  = "md5_digest";
    final static String FIELD_MIME_TYPE   = "mime_type";
    final static String FIELD_PARENT      = "blob_parent";
    final static String FIELD_PART_NAME   = "blob_name";
    final static String FIELD_STG_PATH    = "s3_path";
    final static String FIELD_STR_VAL     = "str_val";
    final static String FIELD_RELTYPE     = "rel_type";
    final static String FIELD_LEFTID      = "logid_left";
    final static String FIELD_RIGHTID     = "logid_right";

    final static String TABLE_BLOBS       = "blobmgr_blobs";
    final static String TABLE_META        = "blobmgr_meta";
    final static String TABLE_DEPS        = "blobmgr_deps";

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

        macros.put("FIELD_BLOB_ID",     FIELD_BLOB_ID);
        macros.put("FIELD_BLOB_PARENT", FIELD_BLOB_PARENT);
        macros.put("FIELD_BLOB_UUID",   FIELD_BLOB_UUID);
        macros.put("FIELD_BOOL_VAL",    FIELD_BOOL_VAL);
        macros.put("FIELD_CREATED_AT",  FIELD_CREATED_AT);
        macros.put("FIELD_CREATED_BY",  FIELD_CREATED_BY);
        macros.put("FIELD_DBL_VAL",     FIELD_DBL_VAL);
        macros.put("FIELD_EXTRA1_META", FIELD_EXTRA1_META);
        macros.put("FIELD_EXTRA2_META", FIELD_EXTRA2_META);
        macros.put("FIELD_INT_VAL",     FIELD_INT_VAL);
        macros.put("FIELD_IS_DELETED",  FIELD_IS_DELETED);
        macros.put("FIELD_IS_LOCKED",   FIELD_IS_LOCKED);
        macros.put("FIELD_KEY_NAME",    FIELD_KEY_NAME);
        macros.put("FIELD_MD5_DIGEST",  FIELD_MD5_DIGEST);
        macros.put("FIELD_MIME_TYPE",   FIELD_MIME_TYPE);
        macros.put("FIELD_PARENT",      FIELD_PARENT);
        macros.put("FIELD_PART_NAME",   FIELD_PART_NAME);
        macros.put("FIELD_STG_PATH",    FIELD_STG_PATH);
        macros.put("FIELD_STR_VAL",     FIELD_STR_VAL);
        macros.put("FIELD_RELTYPE",     FIELD_RELTYPE);
        macros.put("FIELD_LEFTID",      FIELD_LEFTID);
        macros.put("FIELD_RIGHTID",     FIELD_RIGHTID);
        macros.put("TABLE_BLOBS",       TABLE_BLOBS);
        macros.put("TABLE_META",        TABLE_META);
        macros.put("TABLE_DEPS",        TABLE_DEPS);

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
     * Map from external view/name of core fields to actual RDB table column names
     */
    static final Map<Query.CoreField, String> cCoreMap = new HashMap<>();
    static
    {
        cCoreMap.put(Query.CoreField.CREATOR,   FIELD_CREATED_BY);
        cCoreMap.put(Query.CoreField.CREATEDAT, FIELD_CREATED_AT);
        cCoreMap.put(Query.CoreField.DELETED,   FIELD_IS_DELETED);
        //?? add S3PATH ??
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
    private JdbcDS             jdbcDS;
    private JdbcTransaction    jdbcTx;
    private EntitiesDS<Blob>   blobDS;
    private EntitiesDS<Meta>   metaDS;
    private EntitiesDS<Deps>   depsDS;

    /**
     * Meta is a class used only internally to restore rows from the blobmgr_meta table
     */
    private static class Meta
    {
        int    blobID;
        String keyName;

        // extensible area, one per type supported
        String  strValue;
        Boolean boolValue;
        Integer intValue;
        Double  dblValue;

        Meta(int blobID, String keyName, String strValue, Boolean boolValue, Integer intValue, Double dblValue)
        {
            this.blobID  = blobID;
            this.keyName = keyName;

            this.strValue  = strValue;
            this.boolValue = boolValue;
            this.intValue  = intValue;
            this.dblValue  = dblValue;
        }
    }

    /**
     * Construct a Meta instance for the db row
     */
    private static class MetaRowMapper implements RowMapper<Meta>
    {
        @Override
        public Meta mapRow(ResultSet rs, int rowNum) throws SQLException
        {
            return new Meta(
                rs.getInt(FIELD_BLOB_ID),
                rs.getString(FIELD_KEY_NAME),

                rs.getString(FIELD_STR_VAL),
                rs.getBoolean(FIELD_BOOL_VAL),
                rs.getInt(FIELD_INT_VAL),
                rs.getDouble(FIELD_DBL_VAL)
                );
        }
    }

    /**
     * Deps is a class used only internally to restore rows from the blobmgr_deps table
     */
    private static class Deps
    {
        Integer      blobID;      // dbid (optional)
        String       leftID;      // optional
        Boolean      locked;
        RelationType relType;
        String       rightID;

        Deps(Integer blobID, String leftID, Boolean locked, RelationType relType, String rightID)
        {
            this.blobID  = blobID;
            this.leftID  = leftID;
            this.locked  = locked;
            this.relType = relType;
            this.rightID = rightID;
        }
    }

    /**
     * Construct a Deps instance for the db row
     */
    private static class DepsRowMapper implements RowMapper<Deps>
    {
        @Override
        public Deps mapRow(ResultSet rs, int rowNum) throws SQLException
        {
            return new Deps(
                (Integer) rs.getObject(FIELD_BLOB_ID),
                rs.getString(FIELD_LEFTID),
                DbUtil.getBoolean(rs, FIELD_IS_LOCKED, false),
                RelationType.valueOf(rs.getString(FIELD_RELTYPE).toUpperCase()),
                rs.getString(FIELD_RIGHTID)
                );
        }
    }

    /**
     * select blob by blob_uuid
     */
    private final static String R_BLOBS = makeSql("select * from {TABLE_BLOBS} where {FIELD_BLOB_UUID} = :{FIELD_BLOB_UUID}");

    /**
     * select blob by blob_id in list
     */
    private final static String R_BLOBS_IN_LIST = makeSql("select * from {TABLE_BLOBS}");

    /**
     * select part by parent
     */
    private final static String R_PARTS = makeSql("select * from {TABLE_BLOBS} where {FIELD_PARENT} = :{FIELD_PARENT}");

    /**
     * select part by parent & name
     */
    private final static String R_PART = makeSql("select * from {TABLE_BLOBS} where {FIELD_PARENT} = :{FIELD_PARENT} and {FIELD_PART_NAME} = :{FIELD_PART_NAME}");

    /**
     * select blobs by md5 hash
     */
    private final static String R_BLOBS_BY_MD5 = makeSql("select * from {TABLE_BLOBS} where {FIELD_MD5_DIGEST} = :{FIELD_MD5_DIGEST}");

    /**
     * select from meta table
     */
    private final static String R_META = makeSql("select * from {TABLE_META}");

    /**
     * Delete from meta table
     */
    private final static String D_META = makeSql("delete from {TABLE_META} where {FIELD_BLOB_ID} = :{FIELD_BLOB_ID}");

    private final static String D_META_KEY = makeSql("delete from {TABLE_META} where {FIELD_BLOB_ID} = :{FIELD_BLOB_ID} and {FIELD_KEY_NAME} = :{FIELD_KEY_NAME}");

    /**
     * Overall blob deletion
     */
    private final static String D_PARENT_ID = makeSql(
        "select {FIELD_BLOB_ID} from {TABLE_BLOBS} where {FIELD_BLOB_PARENT} = :{FIELD_BLOB_PARENT} and" +
        " {FIELD_PART_NAME} = :{FIELD_PART_NAME}"
        );

    // returns only non-shared s3_paths!
    private final static String D_PART_S3 = makeSql(
        "select {FIELD_STG_PATH} from {TABLE_BLOBS} where {FIELD_BLOB_ID} = :{FIELD_BLOB_ID} and" +
        " {FIELD_STG_PATH} not in (select {FIELD_STG_PATH} from {TABLE_BLOBS} where {FIELD_STG_PATH} is not null and {FIELD_BLOB_ID} != :{FIELD_BLOB_ID})"
        );

    private final static String D_PART_META = makeSql(
        "delete from {TABLE_META} where {FIELD_BLOB_ID} = :{FIELD_BLOB_ID}"
        );

    private final static String D_PART_DEPS = makeSql(
        "delete from {TABLE_DEPS} where {FIELD_BLOB_ID} = :{FIELD_BLOB_ID}"
        );

    private final static String D_PART_BLOB = makeSql(
        "delete from {TABLE_BLOBS} where {FIELD_BLOB_ID} = :{FIELD_BLOB_ID}"
        );

    // returns only non-shared s3_paths!
    private final static String D_BLOB_S3 = makeSql(
        "select {FIELD_STG_PATH} from {TABLE_BLOBS} where {FIELD_BLOB_PARENT} = :{FIELD_BLOB_PARENT} and" +
        " {FIELD_STG_PATH} not in (select {FIELD_STG_PATH} from {TABLE_BLOBS} where {FIELD_STG_PATH} is not null and {FIELD_BLOB_PARENT} != :{FIELD_BLOB_PARENT})"
        );

    private final static String D_BLOB_META = makeSql(
        "delete from {TABLE_META} where {FIELD_BLOB_ID} in" +
        " (select {FIELD_BLOB_ID} from {TABLE_BLOBS} where {FIELD_BLOB_PARENT} = :{FIELD_BLOB_PARENT} or {FIELD_BLOB_ID} = :{FIELD_BLOB_PARENT})"
        );

    private final static String D_BLOB_DEPS = makeSql(
        "delete from {TABLE_DEPS} where {FIELD_BLOB_ID} in" +
        " (select {FIELD_BLOB_ID} from {TABLE_BLOBS} where {FIELD_BLOB_PARENT} = :{FIELD_BLOB_PARENT} or {FIELD_BLOB_ID} = :{FIELD_BLOB_PARENT})"
        );

    private final static String D_BLOB_BLOB = makeSql(
        "delete from {TABLE_BLOBS} where {FIELD_BLOB_ID} = :{FIELD_BLOB_ID} or {FIELD_BLOB_PARENT} = :{FIELD_BLOB_ID}"
        );

    /**
     * Operations on deps table
     */
    private final static String C_DEPS_BY_OWNER = makeSql("insert into {TABLE_DEPS}" +
                                                          " ({FIELD_BLOB_ID}, {FIELD_RELTYPE}, {FIELD_RIGHTID}) values (:{FIELD_BLOB_ID}, :{FIELD_RELTYPE}, :{FIELD_RIGHTID})");

    private final static String R_BLOB_BY_OWNER = makeSql("select * from {TABLE_BLOBS} where {FIELD_BLOB_ID} in (" +
                                                          " select distinct {FIELD_BLOB_ID} from {TABLE_DEPS} where {FIELD_RELTYPE} = '%s' and" + // "distinct" since we can't enforce non-dups
                                                          " {FIELD_RIGHTID} = :{FIELD_RIGHTID})",
                                                          RelationType.OWNS.toString());

    private final static String R_DEPS_BY_OWNER = makeSql("select * from {TABLE_DEPS} where {FIELD_RELTYPE} = '%s' and {FIELD_BLOB_ID} = :{FIELD_BLOB_ID}",
                                                          RelationType.OWNS.toString());

    // read raw deps
    private final static String R_DEPS_BY_USES = makeSql("select distinct * from {TABLE_DEPS} " +
                                                         "where {FIELD_RELTYPE} = '%s' and " +
                                                         "{FIELD_LEFTID} = :{FIELD_LEFTID}",
                                                         RelationType.USES.toString());

    // read blobs from deps; ugly as we need to do logical-to-owner query AND query from _blobs
    private final static String R_BLOBS_BY_USES = makeSql("select * from {TABLE_BLOBS}" +
                                                          " where {FIELD_BLOB_ID} in (" +
                                                          " select {FIELD_BLOB_ID} from {TABLE_DEPS} where " +
                                                          " {FIELD_RELTYPE} = 'OWNS' and {FIELD_RIGHTID} in (" +
                                                          " select {FIELD_RIGHTID} from {TABLE_DEPS} where {FIELD_RELTYPE} = 'USES' and {FIELD_LEFTID} = :{FIELD_LEFTID}))");

    private final static String R_DEPS_BY_USEDBY = makeSql("select distinct * from {TABLE_DEPS} " +
                                                           "where {FIELD_RELTYPE} = '%s' and " +
                                                           "{FIELD_RIGHTID} = :{FIELD_RIGHTID}",
                                                           RelationType.USES.toString());

    // read blobs from deps; ugly as we need to do logical-to-owner query AND query from _blobs
    private final static String R_BLOBS_BY_USEDBY = makeSql("select * from {TABLE_BLOBS}" +
                                                            " where {FIELD_BLOB_ID} in (" +
                                                            " select {FIELD_BLOB_ID} from {TABLE_DEPS} where " +
                                                            " {FIELD_RELTYPE} = 'OWNS' and {FIELD_RIGHTID} in (" +
                                                            " select {FIELD_LEFTID} from {TABLE_DEPS} where {FIELD_RELTYPE} = 'USES' and {FIELD_RIGHTID} = :{FIELD_RIGHTID}))");

    private final static String C_DEPS_BY_USES = makeSql("insert into {TABLE_DEPS}" +
                                                         " ({FIELD_LEFTID}, {FIELD_RELTYPE}, {FIELD_RIGHTID}) values (:{FIELD_LEFTID}, '%s', :{FIELD_RIGHTID})",
                                                         RelationType.USES.toString());

    private final static String D_DEPS_BY_USES = makeSql("delete from {TABLE_DEPS} where {FIELD_RELTYPE} = '%s' " +
                                                         " and {FIELD_LEFTID} = :{FIELD_LEFTID}",
                                                         RelationType.USES.toString());

    private final static String D_DEPS_BY_OWNED = makeSql("delete from {TABLE_DEPS} where {FIELD_RELTYPE} = '%s' " +
                                                          " and {FIELD_RIGHTID} = :{FIELD_RIGHTID}",
                                                          RelationType.OWNS.toString());

    // dependency locking related.
    private final static String LOCKED_CHAR1  = "y";  // must match constants in sql here:
    private final static String C_DEPS_LOCK   = makeSql("update {TABLE_DEPS} set {FIELD_IS_LOCKED} = 'y' where {FIELD_LEFTID} = :{FIELD_LEFTID}");
    private final static String R_DEPS_LOCKED = makeSql("select 'y' from {TABLE_DEPS} where {FIELD_LEFTID} = :{FIELD_LEFTID} and {FIELD_IS_LOCKED} = 'y'");

    /**
     * DbTable takes care of some of the headache of forming SQL + binding info
     */
    private final static DbTable DBT_BLOBS = new DbTable<Blob.Fields, Blob>(
        TABLE_BLOBS,

        // these fields require transformation of the value returned from the class' getter
        DbField.viaExtractor(FIELD_EXTRA1_META, Blob.Fields.EXTRA1,      new DbField.Extractor<Blob>() { public Object get(Blob pojo) { return DbUtil.putJson(pojo.getExtra1());         }}),
        DbField.viaExtractor(FIELD_EXTRA2_META, Blob.Fields.EXTRA2,      new DbField.Extractor<Blob>() { public Object get(Blob pojo) { return DbUtil.putJson(pojo.getExtra2());         }}),
        DbField.viaExtractor(FIELD_BLOB_UUID,   Blob.Fields.UUID,        new DbField.Extractor<Blob>() { public Object get(Blob pojo) { return DbUtil.putXuuid(pojo.getUuid());          }}),
        DbField.viaExtractor(FIELD_CREATED_AT,  Blob.Fields.CREATEDAT,   new DbField.Extractor<Blob>() { public Object get(Blob pojo) { return DbUtil.putTimestamp(pojo.getCreatedAt()); }}),
        DbField.viaExtractor(FIELD_IS_DELETED,  Blob.Fields.SOFTDELETED, new DbField.Extractor<Blob>() { public Object get(Blob pojo) { return DbUtil.putBoolean(pojo.getSoftDeleted()); }}),

        // these fields don't require transformation and therefore can directly access class' getter
        DbField.viaMethodReference(FIELD_CREATED_BY,  Blob.Fields.CREATEDBY,   Blob::getCreatedBy),
        DbField.viaMethodReference(FIELD_STG_PATH,    Blob.Fields.STORAGEPATH, Blob::getStoragePath),
        DbField.viaMethodReference(FIELD_MD5_DIGEST,  Blob.Fields.MD5DIGEST,   Blob::getMd5Digest),
        DbField.viaMethodReference(FIELD_PARENT,      Blob.Fields.PARENT,      Blob::getParentDbID),
        DbField.viaMethodReference(FIELD_PART_NAME,   Blob.Fields.PARTNAME,    Blob::getPartName),
        DbField.viaMethodReference(FIELD_MIME_TYPE,   Blob.Fields.MIMETYPE,    Blob::getMimeType)

        ).setIdField(DbField.viaMethodReference(FIELD_BLOB_ID, Blob.Fields.ID, Blob::getDbID));

    /**
     * For reading in rows from TABLE_BLOBS
     */
    private static class BlobRowMapper implements RowMapper<Blob>
    {
        private Blob parent;

        BlobRowMapper()
        {
            this(null);
        }

        BlobRowMapper(Blob parent)
        {
            this.parent = parent;
        }

        @Override
        public Blob mapRow(ResultSet rs, int rowNum) throws SQLException
        {
            return Blob.fromPersisted(
                rs.getInt(FIELD_BLOB_ID),
                DbUtil.getTimestamp(rs, FIELD_CREATED_AT, true),
                rs.getString(FIELD_CREATED_BY),
                DbUtil.getBoolean(rs, FIELD_IS_DELETED, true).booleanValue(),  // required so .booleanValue will work

                DbUtil.getXuuid(rs, FIELD_BLOB_UUID, false),   // not required
                rs.getInt(FIELD_PARENT),
                parent,
                rs.getString(FIELD_PART_NAME),

                rs.getString(FIELD_STG_PATH),
                rs.getString(FIELD_MD5_DIGEST),
                rs.getString(FIELD_MIME_TYPE),

                DbUtil.getJson(rs.getString(FIELD_EXTRA1_META)),
                DbUtil.getJson(rs.getString(FIELD_EXTRA2_META))
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
            jdbcDS = initContext.daoServices.getJdbc("blobmanager");
            jdbcTx = jdbcDS.getTransactionManager();

            blobDS = new EntitiesDS<Blob>(jdbcDS, new BlobRowMapper(), null);
            metaDS = new EntitiesDS<Meta>(jdbcDS, new MetaRowMapper(), null);  // somewhat overkill
            depsDS = new EntitiesDS<Deps>(jdbcDS, new DepsRowMapper(), null);
        }
    }

    /**
     * Unfortunately DB transactional support is required... typical model but nested
     * transactions are not supported.  Callers of beginTransaction MUST call either
     * abortTransaction or commitTransaction.  Transactions are managed on a per-thread
     * basis.
     *
     * Because this code doesn't want to assume what other methods the caller might or
     * might not call, all methods in this class that require transactional support
     * themselves must check if we're already in a transaction and skip the begin()
     * at their level
     */
    @Override
    public void beginTransaction()
    {
        jdbcTx.begin();
    }

    @Override
    public void commitTransaction()
    {
        jdbcTx.commit();
    }

    @Override
    public void abortTransaction()
    {
        jdbcTx.abort();
    }

    /**
     * Creates a new row in blob table.  blob.xuuid must not already exist
     */
    @Override
    public void createMetadata(Blob blob)
    {
        TxWrapper tx = new TxWrapper();

        // assumption is that caller will have checked conformance to metadata "schema" if desired

        try
        {
            SqlStatement sql = DBT_BLOBS.createSqlInsert(blob);
    
            blob.setDbID(blobDS.createEntity(sql.sql, sql.binding, true));

            saveMetadata(blob);

            tx.commit();
        }
        catch (Throwable t)
        {
            tx.abort();

            throw new RuntimeException("Failure to create Blob metadata", t);
        }
    }

    /**
     * Updates the given blob's metadata--both table- and json-based
     */
    public void updateMetadata(Blob blob, boolean replace)
    {
        TxWrapper tx = new TxWrapper();

        try
        {
            SqlStatement sql = DBT_BLOBS.createSqlUpdate(blob, blob.getModified());

            if (sql != null)  // null iff nothing to update
                blobDS.updateEntity(sql.sql, sql.binding);

            if (blob.getModified().contains(Blob.Fields.METADATA))
            {
                // metadata was touched so remove existing and add new
                removeMetadata(blob, replace);
                saveMetadata(blob);
            }

            tx.commit();
        }
        catch (Throwable t)
        {
            tx.abort();

            throw new RuntimeException("Failure to update Blob metadata", t);
        }
    }

    /**
     *
     */
    public Blob getByBlobID(Schema schema, XUUID id)
    {
        Blob blob = blobDS.loadEntity(R_BLOBS, Binding.byName(FIELD_BLOB_UUID, id.toString()));

        if (blob != null)
            readMetadata(schema, Arrays.asList(blob));

        return blob;
    }

    /**
     * Finds and restores the blob that has the given parent blob and has the
     * given part name.
     */
    public Blob getByPartName(Schema schema, Blob parent, String partName)
    {
        EntitiesDS<Blob> partDS = new EntitiesDS<Blob>(jdbcDS, new BlobRowMapper(parent), null);
        Binding          bind   = Binding.byName(FIELD_PARENT, parent.getDbID()).addByName(FIELD_PART_NAME, partName);
        Blob             part   = partDS.loadEntity(R_PART, bind);

        if (part != null)
            readMetadata(schema, Arrays.asList(part));

        return part;
    }

    /**
     * Return list of children blobs; empty list if none.
     */
    public List<Blob> getParts(Blob parent)
    {
        EntitiesDS<Blob> partDS = new EntitiesDS<Blob>(jdbcDS, new BlobRowMapper(parent), null);
        Binding          bind   = Binding.byName(FIELD_PARENT, parent.getDbID());

        // note that we currently don't keep metadata on parts so we don't need to read/assemble
        return partDS.loadEntities(R_PARTS, bind);
    }

    /**
     * Look up the parent/containing blobs for each of the parts in the list.  The returned
     * structure is a map from the part to its parent blob.
     */
    public Map<Blob, Blob> getPartParents(List<Blob> parts)
    {
        if ((parts == null) || (parts.size() == 0))
            return Collections.emptyMap();

        com.apixio.datasource.springjdbc.Query sub          = new com.apixio.datasource.springjdbc.Query(R_BLOBS_IN_LIST, null);
        Map<Blob, Blob>                        partToParent = new HashMap<>();
        List<Blob>                             blobs;

        sub.where(sub.inIntList(FIELD_BLOB_ID, parts.stream().map(b -> b.getRawParentID()).distinct().collect(Collectors.toList())));

        blobs = blobDS.loadEntities(sub.toSql(), null);

        for (Blob part : parts)
        {
            for (Blob parent : blobs)
            {
                if (parent.getDbID() == part.getRawParentID())
                {
                    partToParent.put(part, parent);
                    break;
                }
            }
        }

        return partToParent;
    }

    /**
     * Finds all blobs with the given md5 hash.  Only parts will (theoretically) be
     * returned here.  Metadata is NOT returned here (as it's not needed externally)
     */
    @Override
    public List<Blob> getByMd5Hash(String md5)
    {
        EntitiesDS<Blob> partDS = new EntitiesDS<Blob>(jdbcDS, new BlobRowMapper(), null);
        Binding          bind   = Binding.byName(FIELD_MD5_DIGEST, md5.toUpperCase());
        // toUpperCase makes it compatible with com.apixio.util.Md5DigestInputStream.getMd5ASsHex()
        // which is what's used by BlobManager.uploadPart.

        // note that we currently don't keep metadata on parts so we don't need to read/assemble
        return partDS.loadEntities(R_BLOBS_BY_MD5, bind);
    }

    /**
     * Deletes metadata, non-shared s3 blob data, and blob, for all parts owned by the blob
     * and for the Blob itself
     */
    @Override
    public List<String> deleteBlob(Blob blob)
    {
        Binding      binding  = Binding.byName(FIELD_BLOB_PARENT, blob.getDbID());
        List<String> s3Paths  = blobDS.getValues(D_BLOB_S3, binding, String.class);

        beginTransaction();

        metaDS.deleteEntities(D_BLOB_META, binding);
        depsDS.deleteEntities(D_BLOB_DEPS, binding);

        binding.addByName(FIELD_BLOB_ID, blob.getDbID());  // either this or change the SQL to be unintuitive
        blobDS.deleteEntities(D_BLOB_BLOB, binding);

        commitTransaction();

        return s3Paths;
    }

    /**
     * Deletes metadata, non-shared s3 blob data, and blob.  Return value is overloaded:
     * if non-null, then this indicates success in that the part name was valid and the
     * elements of the returned list are the S3 paths to blob data that need to be deleted.
     */
    @Override
    public List<String> deletePart(Blob parent, String part)
    {
        Integer      blobID   = blobDS.getValue(D_PARENT_ID,
                                                Binding.byName(FIELD_BLOB_PARENT, parent.getDbID()).
                                                addByName(FIELD_PART_NAME, part), Integer.class);
        if (blobID != null)
        {
            Binding      binding  = Binding.byName(FIELD_BLOB_ID, blobID);
            List<String> s3Paths  = blobDS.getValues(D_PART_S3, binding, String.class);

            beginTransaction();

            metaDS.deleteEntities(D_PART_META, binding);
            depsDS.deleteEntities(D_PART_DEPS, binding);
            blobDS.deleteEntities(D_PART_BLOB, binding);

            commitTransaction();

            return s3Paths;
        }

        return null;
    }

    /**
     * Does a generalized query by metadata table and/or JSON (in extra_meta).  Schema is
     * needed to construct real instances of Metadata
     */
    @Override
    public List<Blob> queryMatching(Schema schema, Query q)
    {
        List<Blob> blobs = null;

        try
        {
            SqlStatement sql = q.generateSql();

            blobs = blobDS.loadEntities(sql.sql, sql.binding);

            // now load metadata for each... uglyish
            if (blobs.size() > 0)
                readMetadata(schema, blobs);
        }
        catch (Throwable t)
        {
            throw new RuntimeException("Query failed on sql", t);
        }

        return blobs;
    }

    /**
     * Owner management; only one blob can own a given logicalID (but a given blob can
     * own more than one logicalID).  Note that this method doesn't care about
     * the semantics of locking (we're letting calling business logic decide that...)
     */
    @Override
    public void setOwner(Blob owner, List<String> logicalIDs)
    {
        if (logicalIDs != null)
        {
            Binding   binding = Binding.byName(FIELD_BLOB_ID, owner.getDbID());
            TxWrapper tx      = new TxWrapper();

            binding.addByName(FIELD_RELTYPE, RelationType.OWNS.toString());

            try
            {
                for (String id : logicalIDs)
                {
                    // since we're "reparenting" logical IDs, first un-own from current
                    binding.addByName(FIELD_RIGHTID, id);
                    depsDS.deleteEntities(D_DEPS_BY_OWNED, binding);  // will be at most 1 row

                    // and then reparent
                    binding.addByName(FIELD_RIGHTID, id);
                    depsDS.createEntity(C_DEPS_BY_OWNER, binding);
                }

                tx.commit();
            }
            catch (Throwable t)
            {
                tx.abort();

                throw new RuntimeException("Failure to set owner of logical IDs", t);
            }
        }
    }

    @Override
    public Blob getOwner(Schema schema, String logicalID)
    {
        Binding   binding = Binding.byName(FIELD_RIGHTID, logicalID);
        Blob      blob    = blobDS.loadEntity(R_BLOB_BY_OWNER, binding);

        if (blob != null)
            readMetadata(schema, Arrays.asList(blob));

        return blob;
    }

    @Override
    public List<String> getOwned(Blob owner)
    {
        if (owner == null) {
            return Collections.emptyList();
        }
        return depsDS.loadEntities(R_DEPS_BY_OWNER, Binding.byName(FIELD_BLOB_ID, owner.getDbID())).stream()
            .map(d -> d.rightID)
            .collect(Collectors.toList());
    }

    /**
     * Dependency management
     */
    @Override
    public void addDependencies(String leftID, List<String> rightIDs, boolean replace)
    {
        TxWrapper tx = new TxWrapper();

        try
        {
            Binding binding = Binding.byName(FIELD_LEFTID, leftID);

            if (replace)
                depsDS.deleteEntities(D_DEPS_BY_USES, binding);

            for (String rightID : rightIDs)
            {
                binding.addByName(FIELD_RIGHTID, rightID);
                depsDS.createEntity(C_DEPS_BY_USES, binding);
            }

            tx.commit();
        }
        catch (Throwable t)
        {
            tx.abort();

            throw new RuntimeException("Failure to add dependencies", t);
        }
    }

    /**
     * Performs the dependency query direction specified by "rel":
     *
     *  * USES:     returns set of rightIDs specified in addDependency() for the given logicalID
     *  * USED_BY:  returns set of leftIDs  specified in addDependency() for the given logicalID
     */
    @Override
    public List<String> getLogicalDependencies(String logicalID, Relationship rel)
    {
        if (rel == Relationship.USED_BY)
            return depsDS.loadEntities(R_DEPS_BY_USEDBY,
                                       Binding.byName(FIELD_RIGHTID, logicalID)).stream().map(
                                           d -> d.leftID).collect(Collectors.toList());
        else
            return depsDS.loadEntities(R_DEPS_BY_USES,
                                       Binding.byName(FIELD_LEFTID, logicalID)).stream().map(
                                           d -> d.rightID).collect(Collectors.toList());
    }

    /**
     * Efficient convienence method that does the equivalent of:
     *
     *  return getLogicalDependencies().stream().map(l -> getOwner(l)).collect(Collectors.toList());
     */
    @Override
    public List<Blob> getPhysicalDependencies(Schema schema, String logicalID, Relationship rel)
    {
        List<Blob> blobs;

        if (rel == Relationship.USED_BY)
        {
            //?? does this even make sense?  caller is asking to return owners of the logicalIDs that
            // are used by the given one?  So if logicalID is, say, a signal generatorID, then it'll
            // return the Blobs that own the prediction generators that use that signal generator...

            blobs = blobDS.loadEntities(R_BLOBS_BY_USEDBY, Binding.byName(FIELD_RIGHTID, logicalID));
        }
        else
        {
            blobs = blobDS.loadEntities(R_BLOBS_BY_USES,   Binding.byName(FIELD_LEFTID, logicalID));
        }

        if (blobs.size() > 0)
            readMetadata(schema, blobs);

        return blobs;
    }

    @Override
    public void setLogicalIdLocked(List<String> leftIDs)
    {
        TxWrapper tx = new TxWrapper();

        try
        {
            for (String leftID : leftIDs)
                depsDS.updateEntity(C_DEPS_LOCK, Binding.byName(FIELD_LEFTID, leftID));

            tx.commit();
        }
        catch (Throwable t)
        {
            tx.abort();

            throw new RuntimeException("Failure to lock logical IDs", t);
        }
    }

    @Override
    public boolean isLogicalIdLocked(String leftID)
    {
        String locked = depsDS.getValue(R_DEPS_LOCKED, Binding.byName(FIELD_LEFTID, leftID), String.class);

        return LOCKED_CHAR1.equals(locked);
    }

    /**
     * Reads all the metadata rows for the given list of blobs and reconstructs
     * Metadata instances and hooks them to the Blobs.  This code uses an explicit
     * list of db IDs in the "in (...)" clause so if the number of blobs becomes large
     * a different mechanism should be used.
     */
    private List<Meta> readMetadata(Schema schema, List<Blob> owners)
    {
        com.apixio.datasource.springjdbc.Query sub     = new com.apixio.datasource.springjdbc.Query(R_META, null);
        Map<Integer,Metadata>                  mdMap   = new HashMap<>();
        List<Meta>                             metas;

        sub.where(sub.inIntList(FIELD_BLOB_ID, owners.stream().map(b -> b.getDbID()).collect(Collectors.toList())));

        metas = metaDS.loadEntities(sub.toSql(), null);

        // reassemble into Metadata
        for (Meta meta : metas)
        {
            MetadataDef mdd = schema.getDef(meta.keyName);

            // error is possible if code changes schema
            if (mdd != null)
                findMeta(mdMap, meta.blobID).add(mdd, selectValue(mdd.getType(), meta));
        }

        for (Map.Entry<Integer,Metadata> entry : mdMap.entrySet())
            Blob.setMetadata(findOwner(owners, entry.getKey()), entry.getValue());

        return null;
    }

    private Metadata findMeta(Map<Integer,Metadata> mdMap, Integer blobID)
    {
        Metadata md = mdMap.get(blobID);

        if (md == null)
        {
            md = Metadata.create();
            mdMap.put(blobID, md);
        }

        return md;
    }

    private Object selectValue(MetadataDef.Type type, Meta meta)
    {
        if (type == MetadataDef.Type.STRING)
            return meta.strValue;
        else if (type == MetadataDef.Type.BOOLEAN)
            return meta.boolValue;
        else if (type == MetadataDef.Type.INTEGER)
            return meta.intValue;
        else if (type == MetadataDef.Type.DOUBLE)
            return meta.dblValue;
        else
            throw new IllegalStateException();
    }

    private Blob findOwner(List<Blob> owners, Integer id)
    {
        for (Blob blob : owners)
        {
            if (id.equals(blob.getDbID()))
                return blob;
        }

        return null;
    }

    /**
     * dbID is used iff blob was not persisted yet
     */
    private void saveMetadata(Blob blob)
    {
        Metadata     md;

        if ((md = blob.getMetadata()) != null)
        {
            for (Map.Entry<MetadataDef, Object> entry : md.getAll().entrySet())
            {
                Binding     binding = Binding.byName(FIELD_BLOB_ID, blob.getDbID());  // recreate each time otherwise we accumulate values by name
                MetadataDef def     = entry.getKey();
                Object      val     = entry.getValue();
                String      field   = "programming error";
                String      sql;

                binding.addByName(FIELD_KEY_NAME, def.getKeyName());

                switch (def.getType())
                {
                    case STRING:
                        field = FIELD_STR_VAL;
                        break;
                    case INTEGER:
                        field = FIELD_INT_VAL;
                        break;
                    case DOUBLE:
                        field = FIELD_DBL_VAL;
                        break;
                    case BOOLEAN:
                        field = FIELD_BOOL_VAL;
                        break;

                    default:
                        break;
                }

                binding.addByName(field, val);

                sql = makeSql("insert into {TABLE_META}" +
                              " ({FIELD_BLOB_ID}, {FIELD_KEY_NAME}, %s) values (:{FIELD_BLOB_ID}, :{FIELD_KEY_NAME}, :%s)",
                              field, field);

                metaDS.createEntity(sql, binding);
            }
        }
    }

    /**
     * Deletes all rows in the metadata table for the given Blob.  This method is
     * assumed to be called within a transaction
     */
    private void removeMetadata(Blob blob, boolean replace)
    {
        Binding     binding = Binding.byName(FIELD_BLOB_ID, blob.getDbID());
        Metadata    meta;

        if (replace)
        {
            metaDS.deleteEntities(D_META, binding);
        }
        else if ((meta = blob.getMetadata()) != null)
        {
            // delete only what's currently in blob.getMetadata as the non-replace model
            // is to provide only metadata to be replaced
            for (Map.Entry<MetadataDef, Object> entry : meta.getAll().entrySet())
            {
                binding.addByName(FIELD_KEY_NAME, entry.getKey().getKeyName());

                metaDS.deleteEntities(D_META_KEY, binding);
            }
        }
    }


    // ################################################################

    public static void main(String... args)
    {
        Blob         b   = Blob.newBlob("B", "scott", null, makeJson(), makeJson());
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

}
