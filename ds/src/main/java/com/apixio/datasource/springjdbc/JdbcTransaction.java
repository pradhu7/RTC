package com.apixio.datasource.springjdbc;

import javax.sql.DataSource;

import org.springframework.beans.factory.InitializingBean;

import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

/**
 * A simple transaction model on top of JdbcTemplate that is compatible with
 * Spring JDBC's thread-based transaction support .  The model is for the
 * client to call JdbcTransaction.begin(), followed by normal JDBC operations,
 * followed by either JdbcTransaction.abort() or .commit().  The transaction
 * itself is kept on a thread local so begin/commit/abort must be called on the
 * same thread.
 */
public class JdbcTransaction
{
    /**
     * Keep Spring JDBC's TransactionStatus in a thread local as a marker of
     * "in transaction" since we need it anyway for abort/commit
     */
    private static final ThreadLocal<TransactionStatus> tlTrans = new ThreadLocal<>();

    /**
     *
     */
    private DataSourceTransactionManager txManager;

    /**
     * Default transaction definition is PROPAGATION_REQUIRED (meaning force a new transaction)
     */
    private TransactionDefinition transDef = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRED);

    /**
     *
     */
    public JdbcTransaction(DataSource dataSource)
    {
        this.txManager = new DataSourceTransactionManager(dataSource);
    }

    /**
     * Begin a new transaction boundary.  Nested transactions are not supported.
     */
    public void begin()
    {
        if (inTransaction())
            throw new IllegalStateException("Nested JDBC transactions not currently supported");

        TransactionStatus ts = txManager.getTransaction(transDef);

        tlTrans.set(ts);
    }

    /**
     * Commit an open transaction
     */
    public void commit()
    {
        if (!inTransaction())
            throw new IllegalStateException("Nested JDBC transactions not currently supported");

        try
        {
            txManager.commit(tlTrans.get());
        }
        finally
        {
            tlTrans.set(null);
        }
    }

    /**
     * Abort an option transactions
     */
    public void abort()
    {
        if (!inTransaction())
            throw new IllegalStateException("Nested JDBC transactions not currently supported");

        try
        {
            txManager.rollback(tlTrans.get());
        }
        finally
        {
            tlTrans.set(null);
        }
    }

    /**
     * Returns true if the current thread has opened a transaction.
     */
    public boolean inTransaction()
    {
        return (tlTrans.get() != null);
    }

}
