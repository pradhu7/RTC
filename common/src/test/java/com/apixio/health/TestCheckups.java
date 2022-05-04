package com.apixio.health;

import static org.junit.Assert.*;

import org.junit.Test;

import com.apixio.logger.FluentCheckup;

public class TestCheckups {
    
    @Test
    public void testFluent() {
        String name = FluentCheckup.NAME;
        new FluentCheckup();
        Checkup fluent = Checkups.getCheckups().get(name);
        assertEquals(fluent.getName(), name);
    }

}
