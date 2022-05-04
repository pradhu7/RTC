package com.apixio.logger.tracks;

import com.apixio.logger.tracks.Tracked;

@Tracked(doLog = false)
public class TestLoggedImpl implements TestLogged {
    public String thug1 = null;
    
    @Override
    public void test1(String abc) {
        thug1 = abc;
    }
    
    @Override
    public String getThug1() {
        return thug1;
    }

}
