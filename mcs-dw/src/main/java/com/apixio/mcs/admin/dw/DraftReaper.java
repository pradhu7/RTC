package com.apixio.mcs.admin.dw;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apixio.XUUID;
import com.apixio.bms.Blob;
import com.apixio.bms.Query.Term;
import com.apixio.bms.Query;
import com.apixio.bms.QueryBuilder;
import com.apixio.mcs.Lifecycle;
import com.apixio.mcs.McsMetadataDef;
import com.apixio.mcs.ModelCatalogService;
import com.apixio.mcs.ModelMeta;
import com.apixio.restbase.config.ConfigSet;

class DraftReaper extends Thread
{
    private static final Logger LOG = LoggerFactory.getLogger(DraftReaper.class);

    private final static long HOUR_MS = 1000L * 60 * 60;

    /**
     * Dotted-path config strings
     */
    private final static String CFG_MODE  = "draftReaper.mode";
    private final static String CFG_AGE   = "draftReaper.olderThan";
    private final static String CFG_CHECK = "draftReaper.checkEvery";

    // DELETE will also delete s3 objects!
    private enum Mode { DISABLED, INFO, DELETE }

    private ModelCatalogService catalogSvc;
    private int                 oldAge;
    private long                sleepMs;
    private Mode                runMode;

    DraftReaper(ModelCatalogService mcs, int draftOldDays, int everyXMin)
    {
        runMode    = Mode.INFO;
        catalogSvc = mcs;
        oldAge     = draftOldDays;
        sleepMs    = 1000L * 60 * everyXMin;
    }

    DraftReaper(ModelCatalogService mcs, ConfigSet config)
    {
        catalogSvc = mcs;

        runMode = Mode.valueOf(config.getString(CFG_MODE, Mode.INFO.toString()).toUpperCase());
        oldAge  = config.getInteger(CFG_AGE, 30);
        sleepMs = config.getInteger(CFG_CHECK, 1);

        oldAge  = Math.max(1, oldAge);              // at least 1 day old
        sleepMs = Math.max(1, sleepMs) * HOUR_MS;   // at least 1 hour
    }

    public void run()
    {
        if (runMode == Mode.DISABLED)
        {
            logInfo("not running as it has been disabled");
        }
        else
        {
            logInfo("running in mode " + runMode);
            logInfo("old age defined as " + oldAge + " day(s)");
            logInfo("checking every " + (sleepMs / HOUR_MS) + " hour(s)");

            safeSleep(60 * 1000L);  // give the system a minute to boot up before we start deleting things

            while (true)
            {
                try
                {
                    deleteOldDrafts();
                }
                catch (Exception x)
                {
                    x.printStackTrace();
                }

                safeSleep(sleepMs);
            }
        }
    }

    private void deleteOldDrafts()
    {
        QueryBuilder qb   = new QueryBuilder();
        List<Term>   ands = new ArrayList<>();
        Calendar     cal  = Calendar.getInstance();
        Date         old;

        cal.add(Calendar.DATE, -oldAge);

        old = cal.getTime();

        ands.add(qb.lt(Query.CoreField.CREATEDAT.toString(), old));
        ands.add(qb.eq(McsMetadataDef.MD_STATE.getKeyName(), Lifecycle.DRAFT.toString()));

        qb.term(qb.and(ands));

        for (ModelMeta mc : catalogSvc.searchBlobs(qb.build()))
        {
            Blob blob = catalogSvc.getModelMeta(XUUID.fromString(mc.id));

            if (blob == null)
            {
                logError("Internal error:  MCID " + mc.id + " was returned as old DRAFT but lookup failed");
            }
            else
            {
                if (runMode == Mode.INFO)
                {
                    logInfo("could remove MCID " + mc.id + " but reaper is in INFO mode");
                }
                else
                {
                    logInfo("removing MCID " + mc.id);
                    catalogSvc.deleteModel(blob);
                }
            }
        }
    }

    private void logInfo(String msg)
    {
        msg = "[DraftReaper] INFO " + msg;

        System.out.println(msg);
        LOG.info(msg);
    }

    private void logError(String msg)
    {
        msg = "[DraftReaper] ERROR " + msg;

        System.out.println(msg);
        LOG.error(msg);
    }

    private void safeSleep(long ms)
    {
        long until = System.currentTimeMillis() + ms;
        long delta;

        while ((delta = until - System.currentTimeMillis()) > 0)
        {
            try
            {
                Thread.sleep(delta);
            }
            catch (InterruptedException ix)
            {
            }
        }
    }
}
