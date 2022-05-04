package com.apixio.datasource.cassandra;

import com.apixio.health.Checkup;
import com.apixio.health.Checkups;
import com.datastax.driver.core.Session;

/**
 * Monitor Cassandra up/down status.
 */

public class CassandraCheckup implements Checkup {
    public static final String NAME = "datastax";
    public static final long TIMEOUT = 50;

    private long lastError = 0;

    private final Session session;

    public static void setSession(Session session) {
        new CassandraCheckup(session);
    }

    public CassandraCheckup(Session session) {
        this.session = session;
        Checkups.setCheckup(this);
    }

    @Override
    public String getName() {
        return "datastax";
    }

    @Override
    public boolean isUp() {
        return !session.isClosed() && (System.currentTimeMillis() - lastError > TIMEOUT);
    }

    @Override
    public void status(boolean up) {
        if (up)
            lastError = 0;
        else
            lastError = System.currentTimeMillis();

    }

}
