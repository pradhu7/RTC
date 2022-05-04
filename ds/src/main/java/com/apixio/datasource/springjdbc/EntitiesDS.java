package com.apixio.datasource.springjdbc;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

/**
 * Very simple and generic wrapper over JDBC/RDBMS-land that uses Spring JDBC and doesn't do
 * much to hide key elements of Spring JDBC.  A key concept in Spring's JdbcTemplate is that of
 * a RowMapper, where a row mapper is responsible for taking a row within a JDBC ResultSet and
 * pulling out the fields as necessary to create a POJO.  This wrapper uses that but provides a
 * minimal wrapper over some of the more common operations.
 *
 * This wrapper provides a common interface to the two main mechanisms in Spring JDBC of binding
 * values to SQL statement parameters: by position (supported by JdbcTemplate) and by name
 * (supported by NamedParameterJdbcTemplate).  For named parameters is also double-checks that
 * all parameter names are give in the supplied binding, warning if any are missing.
 *
 * There little benefit in using this class to clients other than the above functionality.
 *
 * The client use-model of this wrapper is to create an instance of one for each distinct mapper
 * and then to use the loadRow(s) methods to execute an SQL query that will return 0 or more
 * rows from the database where the fields produced by the query can be used to create a POJO
 * via the mapper.
 *
 * Non-POJO query results (e.g., "select count(*) from some_table") are supported via the
 * generic method
 *
 * The RowMapper is used ONLY for POJO construction during a query operation.
 *
 * IMPORTANT:  this class is now possibly dependent on MySQL/MariaDB in the handling of the
 * KeyHolder (in createEntity).
 */
public class EntitiesDS<T>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(EntitiesDS.class);

    /**
     * Verbose=true will dump out actual SQL statements with param bindings
     */
    private boolean verboseSql;

    /**
     * This const is the key for a Map<> instance, where the value of this key is the
     * scanned/replaced SQL string in a named-parameter string.  The keys in htis Map<> contain
     * this special ":" and the set of ":{name}" parameter names.  The values for all but
     * the ":" key will be Integer(position)
     */
    private final static String SCANNED_SQL_KEY = ":";

    /**
     * Just because I want to save microseconds...
     */
    private final static Object[] EMPTY_OBJ_ARRAY = new Object[0];

    /**
     *
     */
    private JdbcDS         ds;
    private RowMapper<T>   mapper;
    private RowUnmapper<T> unmapper;

    /**
     * Code uses one of these for actual call, depending on the Binding
     */
    private JdbcTemplate               jdbcTemplate;
    private NamedParameterJdbcTemplate namedTemplate;

    /**
     * Whenever a different Mapper is required, a new instance of EntitiesDS is required.
     */
    public EntitiesDS(JdbcDS ds, RowMapper<T> mapper)
    {
        this(ds, mapper, null);
    }
    public EntitiesDS(JdbcDS ds, RowMapper<T> mapper, RowUnmapper<T> unmapper)
    {
        this.ds       = ds;
        this.mapper   = mapper;
        this.unmapper = unmapper;

        verboseSql    = ds.getVerboseSql();
        jdbcTemplate  = ds.getJdbcTemplate();
        namedTemplate = ds.getNamedParamJdbcTemplate();
    }

    /**
     * Creates an "entity" (row) and returns the created ID/key.  If the 2-parameter form is
     * used or if "useKeyHolder" is false for the 3-parameter form, then the SQL statement
     * is not expected to return a new ID (e.g., via something like an auto-increment field),
     * and a -1 value is returned as the (non-existent row) ID.  If useKeyHolder=true, then
     * the database-supplied ID is returned.
     */
    public void createEntity(String sql, Binding binding)
    {
        createEntity(sql, binding, false);
    }

    public int createEntity(String sql, Binding binding, boolean useKeyHolder)
    {
        Long id = createEntity(sql, binding, useKeyHolder, null, Long.class);

        if (id != null)
            return id.intValue();
        else
            return -1;
    }

    public <T> T createEntity(String sql, Binding binding, boolean useKeyHolder, String idCol, Class<T> idType)
    {
        KeyHolder                 kh  = new GeneratedKeyHolder();
        Number                    id;

        DEBUG("create", sql, binding);

        if (useKeyHolder)
        {
            List<Map<String,Object>> keyList;
            Map<String,Object> keys;

            if (bindingByName(binding))
                namedTemplate.update(sql, new MapSqlParameterSource(binding.getNamedParameters()), kh);
            else
                handleKeyHolderUpdate(sql, binding, kh);

            // keys will be first in the list of per-row fields returned, and the map is from field to value;
            keyList = kh.getKeyList();

            // if we have exactly 1 field returned and caller is asking for an int, then
            // just return it (and let caller handle casting probs)

            // Note:
            //  * keyList size == 0 for "on duplicate update ..." case
            //  * keyList size == 1 for 'normal' insert case
            //  * keyList size  > 1 when "on duplicate update f=v, ..." results in actual update to field of row
            if (keyList.size() != 1)
            {
                // this is expected when an upsert is done (via "on duplicate key update ...")
                debugLog("INFO:  not exactly 1 keyholder produced as part of createEntity." +
                         "  assumption is 'on duplicate key update ...' is being done." +
                         "  keylist=" + keyList);
            }
            else if (((keys = kh.getKeys()) != null) && (keys.size() == 1) && (idType == Long.class))   //!! hack for flavor of createEntity that returns int
            {
                Object val = keys.values().iterator().next();

                if (val == null)
                    throw new IllegalStateException("Expected a non-null value from JDBC insert statement [" + sql + "]");

                // attempt to convert whatever the DB returns to a Long
                if (val instanceof Long)
                {
                    return idType.cast(val);
                }
                else if (val instanceof Integer)
                {
                    debugLog("INFO:  converting keyholder value of type (java.lang.Integer) to Long");
                    return idType.cast(Long.valueOf(((Integer) val).intValue()));
                }
                else if (val instanceof BigInteger)
                {
                    debugLog("INFO:  converting keyholder value of type (java.math.BigInteger) to Long");
                    return idType.cast(Long.valueOf(((BigInteger) val).longValue()));
                }

                throw new IllegalStateException("Unable to cast value returned from JDBC insert statement to Long; value=" + val + " (" + val.getClass().getName() + ")");
            }
            else if (idCol != null)
            {
                return idType.cast(keys.get(idCol));
            }
            else
            {
                throw new IllegalStateException("Failure to create entity:  expected 1 row in KeyHolder [" + keys + "] with idColumn [" + idCol + "]");
            }
        }
        else
        {
            doUpdate(sql, binding);
        }

        return null;
    }

    /**
     * JdbcTemplate suprisingly does NOT have a method to grab generated keys without using a PreparedStatementCreator
     * so this method handles that.
     */
    private void handleKeyHolderUpdate(final String sql, final Binding binding, final KeyHolder kh)
    {
        final Object[] params = (binding != null) ? binding.getByPosition() : EMPTY_OBJ_ARRAY;

        // by position and we need keyholder, but JdbcTemplate is messed up and doesn't have
        // an update() method that takes Object[] *and* KeyHolder so we need to use the
        // PreparedStatementCreator form of update...

        PreparedStatementCreator psc = new PreparedStatementCreator() {
                @Override
                public PreparedStatement createPreparedStatement(Connection conn) throws SQLException
                {
                    PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

                    for (int idx = 0; idx < params.length; idx++)
                        setPreparedStatementPlaceholder(ps, idx + 1, params[idx]);

                    return ps;
                }
            };

        jdbcTemplate.update(psc, kh);
    }

    private void setPreparedStatementPlaceholder(PreparedStatement ps, int pos, Object arg) throws SQLException
    {
        if (arg == null)
            ps.setNull(pos, Types.NULL);
        else if (arg instanceof Boolean)
            ps.setBoolean(pos, (Boolean) arg);
        else if (arg instanceof Integer)
            ps.setInt(pos, (Integer) arg);
        else if (arg instanceof Long)
            ps.setLong(pos, (Long) arg);
        else if (arg instanceof String)
            ps.setString(pos, (String) arg);
        else if (arg instanceof Date)
            ps.setTimestamp(pos, new Timestamp(((Date) arg).getTime()));
        else if (arg instanceof Double)
            ps.setDouble(pos, (Double) arg);
        else if (arg instanceof Float)
            ps.setFloat(pos, (Float) arg);
        else
            throw new IllegalArgumentException("Unable to convert Java type " + arg.getClass() + " to SQL Type");
    }

    /**
     * Equivalent to a createEntity() without returning an ID.
     */
    public void updateEntity(Query query)
    {
        updateEntity(query.toSql(), query.getBinding());
    }
    public void updateEntity(String sql, Binding binding)
    {
        updateEntities(sql, binding);
    }

    /**
     * Deletes an entity(ies) (row/rows)
     */
    public void deleteEntities(Query query)
    {
        deleteEntities(query.toSql(), query.getBinding());
    }
    public void deleteEntities(String sql, Binding binding)
    {
        // 'update' is generic enough to handle update/delete so this method is merely a
        // convenience to provide a meaningful method name
        updateEntities(sql, binding);
    }

    /**
     * Updates an entity(ies) (row/rows)
     */
    public void updateEntities(Query query)
    {
        updateEntities(query.toSql(), query.getBinding());
    }
    public void updateEntities(String sql, Binding binding)
    {
        DEBUG("update", sql, binding);

        doUpdate(sql, binding);
    }

    /**
     * "Entities" are a POJO object that can be created by a RowMapper.  While there usually
     * is a correlation between an RDB table and the POJO, there doesn't need to be (e.g.,
     * the returned fiels from a join operations need to be treated as an entity in this wrapper
     * model).
     */
    public T loadEntity(Query query)
    {
        return loadEntity(query.toSql(), query.getBinding());
    }
    public T loadEntity(String sql, Binding binding)
    {
        List<T> result = loadEntities(sql, binding);
        int     size   = result.size();

        if (size == 0)
            return null;
        else if (size == 1)
            return result.get(0);
        else
            throw new IllegalStateException("Query '" + sql + "' must have returned only 0 or 1 rows but instead returned " + size);
    }

    public List<T> loadEntities(Query query)
    {
        return loadEntities(query.toSql(), query.getBinding());
    }
    public List<T> loadEntities(final String sql, final Binding binding)
    {
        DEBUG("query", sql, binding);

        if (bindingByName(binding))
            return namedTemplate.query(sql, binding.getNamedParameters(), mapper);
        else
            return jdbcTemplate.query(sql, bindingsByPosition(binding), mapper);
    }

    /**
     * Non-entity queries are handled by generically wrapping the appropriate JdbcTemplate.query method.
     * An example "non-entity" would be the int value that's returned from "select count(*) from table".
     */
    public <T> T getValue(Query query, final Class<T> type)
    {
        return getValue(query.toSql(), query.getBinding(), type);
    }
    public <T> T getValue(final String sql, final Binding binding, final Class<T> type)
    {
        ResultSetExtractor<T>    rse;

        rse = new ResultSetExtractor<T>() {
                @Override
                public T extractData(ResultSet rs) throws SQLException
                {
                    if (rs.next())
                        return rs.getObject(1, type);
                    else
                        return null;
                }
            };

        DEBUG("query", sql, binding);

        if (bindingByName(binding))
            return namedTemplate.query(sql, binding.getNamedParameters(), rse);
        else
            return jdbcTemplate.query(sql, bindingsByPosition(binding), rse);
    }

    /**
     * Non-entity queries are handled by generically wrapping the appropriate JdbcTemplate.query method.
     * An example "non-entity" would be the int value that's returned from "select count(*) from table".
     */
    public <T> List<T> getValues(Query query, final Class<T> type)
    {
        return getValues(query.toSql(), query.getBinding(), type);
    }
    public <T> List<T> getValues(final String sql, final Binding binding, final Class<T> type)
    {
        final List<T>               results = new ArrayList<>();
        ResultSetExtractor<List<T>> rse;

        rse = new ResultSetExtractor<List<T>>() {
                @Override
                public List<T> extractData(ResultSet rs) throws SQLException
                {
                    List<T>  results = new ArrayList<>();

                    while (rs.next())
                        results.add(rs.getObject(1, type));

                    return results;
                }
            };

        DEBUG("query", sql, binding);


        if (bindingByName(binding))
            return namedTemplate.query(sql, binding.getNamedParameters(), rse);
        else
            return jdbcTemplate.query(sql, bindingsByPosition(binding), rse);
    }

    /**
     * Noiseless common update code, plus utility methods
     */
    private void doUpdate(String sql, Binding binding)
    {
        if (bindingByName(binding))
            namedTemplate.update(sql, binding.getNamedParameters());
        else
            jdbcTemplate.update(sql, bindingsByPosition(binding));
    }

    private boolean bindingByName(Binding binding)
    {
        return (binding != null) && binding.bindingIsByNamedParams();
    }

    private Object[] bindingsByPosition(Binding binding)
    {
        return (binding != null) ? binding.getByPosition() : null;
    }

    /**
     * dumps out actual SQL (as much as possible, given PreparedStatement & placeholders)
     */
    private void DEBUG(String op, String sql, Binding binding)
    {
        if (verboseSql)
        {
            boolean byName = bindingByName(binding);
            
            if (binding != null)
            {
                if (byName)
                {
                    Map<String, Object>  namedParams  = scanNamedParams(sql);            // name=1, otherfield=2, ...  (i.e., name of namedparam to '?' position, as scanned/parsed)
                    Map<String, Object>  actualParams = binding.getNamedParameters();    // name=theName, otherfield=anothervalue, ... (i.e., actual param values as supplied by client)
                    Map<Integer, Object> valByPos     = new HashMap<>();                 // 1=theName, 2=anothervalue, ...

                    sql = (String) namedParams.remove(SCANNED_SQL_KEY);

                    for (Map.Entry<String, Object> entry : namedParams.entrySet())
                    {
                        Integer pos   = (Integer) entry.getValue();
                        String  pname = entry.getKey();

                        if (!actualParams.containsKey(pname))
                        {
                            debugLog("ERROR:  named parameter '" + pname + "' was not supplied a value via map " + actualParams);
                        }
                        else
                        {
                            Object  val   = actualParams.get(pname);

                            debugLog(" INFO:  adding named param [" + pos + "] with value [" + val + " (" + ((val != null) ? val.getClass().getName() : "<>") + ")]");
                            valByPos.put(pos, val);
                        }
                    }

                    debugLog("Executing " + op + " SQL:  [" + sql + "] with by-name bindings " + valByPos);
                }
                else
                {
                    debugLog("Executing " + op + " SQL:  [" + sql + "] with by-position bindings " + Arrays.asList(bindingsByPosition(binding)));
                }
            }
            else
            {
                debugLog("Executing " + op + " SQL:  [" + sql + "] with no bindings");
            }
        }
    }

    private void debugLog(String msg)
    {
        LOGGER.debug(msg);
    }

    /**
     * Scan for :name params in the string and replace with "?" remembering the logical position
     * of it by its name.  Name contains only alphanumeric and underscore.  SQL constant strings
     * contained with the "'" character are preserved as-is.
     *
     * This scanning operation will modify the resulting SQL string (replacing :name with "?") so
     * the resulting string is returned in the map with the key ":" (since that's an invalid name).
     * This is a semi-hack that forces the use of Map<String,Oject>
     *
     * Note that this could be done more succinctly via regex but this method has better performance
     * (microbenchmarking shows about 2.3x higher perf) and, more importantly, has better error
     * detection and meaningful error messages (e.g., malformed names).
     */
    private static Map<String, Object> scanNamedParams(String sql)
    {
        Map<String, Object> nameToPos = new HashMap<>();
        StringBuilder        outSql    = new StringBuilder();
        int                  len       = sql.length();
        int                  cur       = 0;
        int                  state     = 0;
        int                  position  = 1;    // PreparedStatement positions start at 1
        StringBuilder        name      = null;

        // state0:  outside of :name, search for :
        // state1:  hit :, expect alphanum
        // state2:  in name, end on non-alphanum
        // state3:  hit "'"; skip to matching "'"

        while (cur < len)
        {
            char ch = sql.charAt(cur);

            if (state == 0)
            {
                if (ch == ':')
                {
                    name = new StringBuilder();
                    state = 1;
                    outSql.append("?");
                }
                else if (ch == '\'')
                {
                    outSql.append(ch);
                    state = 3;
                }
                else
                {
                    outSql.append(ch);
                }
            }
            else if (state == 1)
            {
                if (Character.isLetter(ch) || (ch == '_'))
                {
                    name.append(ch);
                    state = 2;
                }
                else
                {
                    throw new IllegalArgumentException("Error in scanning SQL string '" + sql + "' for named parameters:  ':' not followed by letter or _");
                }
            }
            else if (state == 2)
            {
                if (Character.isLetter(ch) || Character.isDigit(ch) || (ch == '_'))
                {
                    name.append(ch);
                }
                else
                {
                    nameToPos.put(name.toString(), Integer.valueOf(position++));
                    outSql.append(ch);
                    state = 0;
                }
            }
            else // state = 3
            {
                outSql.append(ch);

                if (ch == '\'')
                    state = 0;
            }

            cur++;
        }

        if (state == 2)
            nameToPos.put(name.toString(), Integer.valueOf(position));
        else if (state == 1)
            throw new IllegalArgumentException("Error in scanning SQL string '" + sql + "' for named parameters:  isolated ':' found");
        else if (state == 3)
            throw new IllegalArgumentException("Error in scanning SQL string '" + sql + "':  unterminated '");

        nameToPos.put(SCANNED_SQL_KEY, outSql.toString());

        return nameToPos;
    }

    /**
     * test; assumes db named "test" and table "junk (junk_id int primary key auto_increment, junk_name varchar(255))"
     */
    public static void main(String... args)
    {
        JdbcDS             jds = new JdbcDS(makeConfig());
        JdbcTransaction    tx  = null;
        EntitiesDS<Junk>   ds = new EntitiesDS<>(jds, new JunkMapper(), null);
        int                nid;

        Map<String,Object> np = new HashMap<>();
        nid = ds.createEntity("insert into junk (junk_name) values (:name)", Binding.byName("name", "foobynm"), true);

        System.out.println("Entity: "   + ds.loadEntity("select * from junk where junk_id = ?", Binding.byPosition(2)));
        System.out.println("Entities: " + ds.loadEntities("select * from junk", null));
        System.out.println("Count: "    + ds.getValue("select count(*) from junk", null, Integer.class));
        System.out.println("sum: "      + ds.getValue("select sum(junk_id) from junk", null, Integer.class));

        ds.deleteEntities("delete from junk where junk_id = ?", Binding.byPosition(3));
        ds.updateEntities("update junk set junk_name = ? where junk_id = ?", Binding.byPosition("boofoor", 5));
    }

    private static Map<String, Object> makeConfig()
    {
        Map<String, Object> config = new HashMap<>();

        config.put(JdbcDS.JDBC_CONNECTIONURL,   "jdbc:mysql://localhost:3306/testdb");
        config.put(JdbcDS.JDBC_DRIVERCLASSNAME, "com.mysql.jdbc.Driver");
        config.put(JdbcDS.JDBC_USERNAME,        "scott");
        config.put(JdbcDS.JDBC_PASSWORD,        "scott");
        config.put(JdbcDS.SYS_VERBOSESQL,       Boolean.TRUE);

        config.put(JdbcDS.POOL_MAXTOTAL,        25);

        return config;
    }
    private static class Junk
    {
        public int id;
        public String name;

        public Junk(int id, String name)
        {
            this.id   = id;
            this.name = name;
        }

        public String toString()
        {
            return "junk(" + id + ", " + name + ")";
        }
    }

    private static class JunkMapper implements RowMapper
    {
        public Junk mapRow(ResultSet rs, int rowNum) throws SQLException
        {
            return new Junk(rs.getInt(1), rs.getString(2));
        }
    }

}
