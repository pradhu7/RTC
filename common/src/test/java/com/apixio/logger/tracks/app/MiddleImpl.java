package com.apixio.logger.tracks.app;

import com.apixio.logger.tracks.Logged;
import com.apixio.logger.tracks.Tracked;
import com.apixio.logger.tracks.Tracks;


@Tracked(isFrame=false,doLog=true,standard=false)
public class MiddleImpl implements Middle {
    private Top parent;
    private Middle peer;
    private Bottom kid;
    public String name;
    
    MiddleImpl() {
    }
    
    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void knit(Top parent, Middle peer, Bottom kid) {
        this.parent = parent;
        this.peer = peer;
        this.kid = kid;
    }

    @Override
    @Tracks({"middle.self","true"})
    public void callSelf() {
        doNothing();
    }
    
    @Override
    @Tracks({"middle.peer","true"})
    public void callPeer() {
        peer.doNothing();
    }
    
    @Override
    @Tracks({"middle.parent","true"})
    public void callParent() {
        parent.doNothing();
    }

    @Override
    @Tracks({"middle.kid","true"})
    public void callKid() {
        kid.doNothing();
    }
    
    @Override
    @Tracks({"middle.kidparent","true"})
    public void callKidParent() {
        kid.callParent();
    }
    
    @Override
    @Tracks({"middle.nothing", "true"})
    public void doNothing() {
        Logged.put("middle.name", name);
    }
    

}
