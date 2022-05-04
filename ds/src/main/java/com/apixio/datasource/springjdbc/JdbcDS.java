package com.apixio.datasource.springjdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;
import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import static com.apixio.datasource.utility.ConfigUtility.getRequiredConfig;
import static com.apixio.datasource.utility.ConfigUtility.getOptionalConfig;

/**
 * JdbcDS is a thin wrapper over Spring's JdbcTemplate that provides a central
 * place for declaring supported configuration.  It also provides support for
 * a global-ish (i.e., across all clients of a JdbcDS instance) LRU cache of
 * PreparedStatement instances (across all Jdbc Connection objects that come
 * from the DataSource).
 */
public class JdbcDS
{
    /**
     * Fields
     */
    private BasicDataSource dataSource   = new BasicDataSource();
    private boolean         verboseSql;
    private boolean         initialized;

    /**
     * Derived
     */
    private JdbcTemplate                 jdbcTemplate;
    private NamedParameterJdbcTemplate   namedTemplate;
    private JdbcTransaction              transaction;

    /**
     * Supported configuration points, mostly from Apache's DBCP2 config
     */
    public final static String SYS_VERBOSESQL       = "verboseSql";                // type Boolean
    public final static String PS_USEPSPOOL         = "usePreparedStatementPool";  // type Boolean; true
    public final static String PS_PSPOOLMAX         = "maxPreparedStatements";     // type Integer; default 20
    public final static String JDBC_CONNECTIONURL   = "connectionUrl";             // type String; no default
    public final static String JDBC_DRIVERCLASSNAME = "driverClassname";           // type String; no default
    public final static String JDBC_PASSWORD        = "password";                  // type String; no default
    public final static String JDBC_USERNAME        = "username";                  // type String; no default
    public final static String POOL_INITIALSIZE     = "initialSize";               // type Integer
    public final static String POOL_MAXIDLE         = "maxIdle";                   // type Integer
    public final static String POOL_MAXTOTAL        = "maxTotal";                  // type Integer
    public final static String POOL_MINIDLE         = "minIdle";                   // type Integer
    public final static String POOL_TESTONBORROW    = "testOnBorrow";              // type Boolean
    public final static String POOL_TESTWHILEIDLE   = "testwhileIdle";             // type Boolean
    public final static String POOL_VALIDATIONQUERY = "validationQuery";           // type String

    /**
     * init from config (that will likely be from a .yaml file)
     */
    public JdbcDS(Map<String, Object> config)
    {
        // required
        setUrl(getRequiredConfig(config, String.class, JDBC_CONNECTIONURL));

        // optional strings
        setDriverClassName(getOptionalConfig(config, String.class, JDBC_DRIVERCLASSNAME, null));
        setUsername(getOptionalConfig(config, String.class, JDBC_USERNAME,               null));
        setPassword(getOptionalConfig(config, String.class, JDBC_PASSWORD,               null));

        // optional ints
        setInitialSize(getOptionalConfig(config, Integer.class, POOL_INITIALSIZE,  2));
        setMaxIdle(getOptionalConfig(config, Integer.class, POOL_MAXIDLE,         10));
        setMaxTotal(getOptionalConfig(config, Integer.class, POOL_MAXTOTAL,       20));
        setMinIdle(getOptionalConfig(config, Integer.class, POOL_MINIDLE,          2));

        // optional booleans
        setTestOnBorrow(getOptionalConfig(config, Boolean.class, POOL_TESTONBORROW,   true));
        setTestWhileIdle(getOptionalConfig(config, Boolean.class, POOL_TESTWHILEIDLE, true));

        setPoolPreparedStatements(getOptionalConfig(config, Boolean.class, PS_USEPSPOOL,    true));
        setMaxOpenPreparedStatements(getOptionalConfig(config, Integer.class, PS_PSPOOLMAX, 20));

        verboseSql = getOptionalConfig(config, Boolean.class, SYS_VERBOSESQL, false); // debug support


        finish();
    }

    /**
     * init explicitly with setters
     */
    public JdbcDS()
    {
    }

    // DataSource config
    public void setUrl(String url)
    {
        dataSource.setUrl(url);
    }
    public void setDriverClassName(String classname)
    {
        dataSource.setDriverClassName(classname);
    }
    public void setUsername(String username)
    {
        dataSource.setUsername(username);
    }
    public void setPassword(String password)
    {
        dataSource.setPassword(password);
    }
    public void setInitialSize(int size)
    {
        dataSource.setInitialSize(size);
    }
    public void setMaxIdle(int max)
    {
        dataSource.setMaxIdle(max);
    }
    public void setMaxTotal(int max)
    {
        dataSource.setMaxTotal(max);
    }
    public void setMinIdle(int min)
    {
        dataSource.setMinIdle(min);
    }
    public void setTestOnBorrow(boolean test)
    {
        dataSource.setTestOnBorrow(test);
    }
    public void setTestWhileIdle(boolean test)
    {
        dataSource.setTestWhileIdle(test);
    }
    public void setPoolPreparedStatements(boolean pool)
    {
        dataSource.setPoolPreparedStatements(pool);
    }
    public void setMaxOpenPreparedStatements(int maxPsPool)
    {
        dataSource.setMaxOpenPreparedStatements(maxPsPool);
    }

    // misc
    public void setVerboseSql(boolean verbose)
    {
        this.verboseSql = verbose;
    }
    
    /**
     * true => print out SQL before executing real SQL
     */
    public boolean getVerboseSql()
    {
        return verboseSql;
    }

    /**
     * Get the main Spring JDBC template object for the DataSource
     */
    public JdbcTemplate getJdbcTemplate()
    {
        finish();

        return jdbcTemplate;
    }

    public NamedParameterJdbcTemplate getNamedParamJdbcTemplate()
    {
        finish();

        return namedTemplate;
    }

    /**
     *
     */
    public JdbcTransaction getTransactionManager()
    {
        if (transaction == null)
            throw new IllegalStateException("JdbcBase hasn't been initialized yet");

        return transaction;
    }

    /**
     * Actually create the real objects after configuration
     */
    private void finish()
    {
        if (!initialized)
        {
            jdbcTemplate  = new JdbcTemplate(dataSource);
            namedTemplate = new NamedParameterJdbcTemplate(dataSource);
            transaction   = new JdbcTransaction(dataSource);

            initialized = true;
        }
    }

}
