package com.apixio.logger.tracks;

import java.util.Map;

public class Callback {
    public Map<String, String> mdc = null;

    public static void hello() {
        
    }
    
    @Override
    public String toString() {
        if (mdc != null)
            return mdc.toString();
        return "null";
    }
}
