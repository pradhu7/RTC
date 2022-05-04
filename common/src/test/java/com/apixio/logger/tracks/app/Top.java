package com.apixio.logger.tracks.app;

public interface Top {
    
    void callSelf();
    
    void callPeer();
    
    void callKid();
    
    void callGrandKid();
    
    void doNothing();

    void knit(Top peer, Middle middle, Bottom bottom);

    void setName(String name);

}
