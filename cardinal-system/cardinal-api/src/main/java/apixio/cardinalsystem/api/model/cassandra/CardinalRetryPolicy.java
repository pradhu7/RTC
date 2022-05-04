package apixio.cardinalsystem.api.model.cassandra;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.WriteType;
import com.datastax.driver.core.exceptions.DriverException;
import com.datastax.driver.core.policies.RetryPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Copy of ApixioRetryPolicy. Copying to avoid pulling in a large dependency tree from datasource
 */
public class CardinalRetryPolicy implements RetryPolicy {
    private final static long baseSleep = 100L;
    private final static long maxSleep = 1000L;
    private final static int attemptsMax = 10;

    private static Logger logger = LoggerFactory.getLogger(CardinalRetryPolicy.class);

    private int readAttempts;
    private int writeAttempts;
    private int unavailableAttempts;
    private boolean downgrade;

    public CardinalRetryPolicy(int readAttempts, int writeAttempts, int unavailableAttempts, boolean downgrade) {
        this.readAttempts = readAttempts;
        this.writeAttempts = writeAttempts;
        this.unavailableAttempts = unavailableAttempts;
        this.downgrade = downgrade;

        setDefaultValuesIfNecessary();
    }

    void setDefaultValuesIfNecessary() {
        if (readAttempts == 0) readAttempts = attemptsMax;
        if (writeAttempts == 0) writeAttempts = attemptsMax;
        if (unavailableAttempts == 0) unavailableAttempts = attemptsMax;
    }

    @Override
    public RetryDecision onReadTimeout(Statement statement, ConsistencyLevel cl, int requiredResponses, int receivedResponses, boolean dataRetrieved, int nbRetry) {
        if (nbRetry < readAttempts) {
            errorLog("onReadTimeout", statement, cl, nbRetry);
            sleep(nbRetry);
            return RetryDecision.retry(cl);
        } else if (downgrade && (nbRetry < 2 * readAttempts)) {
            errorLog("onReadTimeout", statement, ConsistencyLevel.ONE, nbRetry);
            sleep(nbRetry);
            return RetryDecision.retry(ConsistencyLevel.ONE);
        } else {
            errorLog("onReadTimeout", statement, null, nbRetry);
            return RetryDecision.rethrow();
        }
    }

    @Override
    public RetryDecision onWriteTimeout(Statement statement, ConsistencyLevel cl, WriteType wt, int requiredResponses, int receivedResponses, int nbRetry) {
        if (nbRetry < writeAttempts) {
            errorLog("onWriteTimeout", statement, cl, nbRetry);
            sleep(nbRetry);
            return RetryDecision.retry(cl);
        } else {
            errorLog("onWriteTimeout", statement, null, nbRetry);
            return RetryDecision.rethrow();
        }
    }

    @Override
    public RetryDecision onUnavailable(Statement statement, ConsistencyLevel cl, int requiredResponses, int receivedResponses, int nbRetry) {
        if (nbRetry < unavailableAttempts) {
            errorLog("onUnavailable", statement, cl, nbRetry);
            sleep(nbRetry);
            return RetryDecision.tryNextHost(cl);
        } else {
            errorLog("onUnavailable", statement, ConsistencyLevel.ONE, nbRetry);
            return RetryDecision.rethrow();
        }
    }

    public RetryDecision onRequestError(Statement statement, ConsistencyLevel cl, DriverException e, int nbRetry) {
        if (nbRetry < unavailableAttempts) {
            errorLog("onRequestError", statement, cl, nbRetry);
            sleep(nbRetry);
            return RetryDecision.tryNextHost(cl);
        } else {
            errorLog("onRequestError", statement, ConsistencyLevel.ONE, nbRetry);
            return RetryDecision.rethrow();
        }
    }

    private void errorLog(String call, Statement statement, ConsistencyLevel cl, int retry) {
        String c = cl != null ? cl.name() : "RETROW";

        try {
            logger.error("Failed: Attempt number (" + retry + "); Retry call type (" + call + "); Consistency level (" + c +
                    "); Statement (" + toStringWithValues((BoundStatement) statement) + ")");
        } catch (Exception e) {
            logger.error("Failed logging the call: " + call);
        }
    }

    private String toStringWithValues(BoundStatement boundStatement) {
        return boundStatement.toString();
    }

    private final static void sleep(int attempts) {
        long delay = Math.min(baseSleep * (1L << attempts++), maxSleep);

        try {
            Thread.sleep(delay);
        } catch (InterruptedException ix) {
        }
    }

    @Override
    public void init(Cluster cluster) {
        // nothing to do
    }

    @Override
    public void close() {
        // nothing to do
    }
}
