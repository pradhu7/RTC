package com.apixio.datasource.cassandra;

/**
 * MonitoredCqlCrud keeps track of timing/count stats to help detect overuse of
 * CqlCrud calls and/or slow CqlCrud calls.  The stats/detection are really
 * only useful within the context of a single RESTful call so this class
 * automatically resets things at the beginning of a CQL transaction (which is
 * started automatically at the beginning of a REST call) and dumps out info at
 * the end of the transaction.
 */
public class MonitoredCqlCrud extends CqlTransactionCrud {

    private CqlMonitor cqlMonitor;

    public MonitoredCqlCrud(boolean detailed, boolean enableTrace)
    {
        super();

        cqlMonitor = new CqlMonitor(detailed);

        setCqlMonitor(cqlMonitor);
        super.setTraceEnabled(enableTrace);
    }

    /**
     * Allows you to modify the detailed stats level
     *
     * @param detailed
     */
    private void setDetailedStats(boolean detailed)
    {
        cqlMonitor.setDetailedStats(detailed);
    }

    /**
     * Intercept obvious begin/end points so we can reset/dump stats
     */

    @Override
    public void beginTransaction()
    {
        super.beginTransaction();

        cqlMonitor.startMonitoring();
    }

    @Override
    public void commitTransaction()
    {
        super.commitTransaction();

        cqlMonitor.dumpMonitoring();
    }

    @Override
    public void abortTransaction()
    {
        super.abortTransaction();

        cqlMonitor.dumpMonitoring();
    }
}
