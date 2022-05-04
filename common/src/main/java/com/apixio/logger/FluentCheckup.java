package com.apixio.logger;

import com.apixio.health.Checkup;
import com.apixio.health.Checkups;

/**
 * Checkup for Fluent
 * 
 * @author lance
 *
 */

public class FluentCheckup implements Checkup {
    public static final String NAME = "fluent";
    
    public static void initCheckup() {
        Checkup checkup = Checkups.getCheckup(NAME);
        if (checkup == null)
             Checkups.setCheckup(new FluentCheckup());
    }

    public FluentCheckup() {
        Checkups.setCheckup(this);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean isUp() {
        return EventLogger.isUp();
    }

    @Override
    public void status(boolean up) {
        
    }

}
