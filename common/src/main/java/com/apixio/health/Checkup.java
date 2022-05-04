package com.apixio.health;

/**
 * Check the health of a component.
 * 
 * @author lance
 *
 */

public interface Checkup {

    public String getName();
    
    public boolean isUp();
    
    public void status(boolean up);
    
}
