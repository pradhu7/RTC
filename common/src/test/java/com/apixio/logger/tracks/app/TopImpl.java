package com.apixio.logger.tracks.app;

import com.apixio.logger.tracks.Logged;
import com.apixio.logger.tracks.Tracked;
import com.apixio.logger.tracks.Tracks;

@Tracked(isFrame=true,doLog=true,standard=false)
public class TopImpl implements Top {
    private Top peer;
    private Middle kid;
    private Bottom grandKid;
    private String name;

    TopImpl() {
    }
    
    @Override
    public void setName(String name) {
        this.name = name;
    }
    
    @Override
    public void knit(Top peer, Middle middle, Bottom bottom) {
        this.peer = peer;
        this.kid = middle;
        this.grandKid = bottom;
    }

    @Override
    @Tracks({"top.self","true"})
    public void callSelf() {
       this.doNothing();
    }
    
    @Override
    @Tracks({"top.peer", "true"})
    public void callPeer() {
        peer.doNothing();
    }

    @Override
    @Tracks({"top.kid","true"})
    public void callKid() {
        kid.doNothing();
    }

    @Override
    @Tracks({"top.grandkid","true"})
    public void callGrandKid() {
        grandKid.doNothing();
    }

    @Override
    @Tracks({"top.nothing", "true"})
    public void doNothing() {
        Logged.put("top.name", name);
    }

}
