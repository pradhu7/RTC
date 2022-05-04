package com.apixio.logger.tracks.app;

public interface Middle {

    void callSelf();
    
    void callPeer();

    void callParent();

    void callKid();

    void callKidParent();

    void doNothing();

    void knit(Top top, Middle middle, Bottom bottom);

    void setName(String name);

}
