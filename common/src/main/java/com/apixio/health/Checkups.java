package com.apixio.health;

import java.util.HashMap;
import java.util.Map;

/**
 * Manager for Health Check services.
 * 
 * @author lance
 *
 */

public class Checkups {
    private static final Map<String, Checkup> services = new HashMap<>();
    
    static {
        System.out.println("init checkups");
    }
    
    public static void setCheckup(Checkup checkup) {
        services.put(checkup.getName(), checkup);
    }
    
    public static Map<String, Checkup> getCheckups() {
        return services;
    }
    
    public static Checkup getCheckup(String name) {
        return services.get(name);
    }
}
