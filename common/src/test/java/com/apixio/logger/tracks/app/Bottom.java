package com.apixio.logger.tracks.app;

public interface Bottom {
    
    void callSelf();
    
    void callParent();

    void doNothing();

    void knit(Middle middle);
}
