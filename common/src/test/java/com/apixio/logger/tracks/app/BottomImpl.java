package com.apixio.logger.tracks.app;

import com.apixio.logger.tracks.Tracked;
import com.apixio.logger.tracks.Tracks;


@Tracked(isFrame=false, doLog=false,standard=false)
public class BottomImpl implements Bottom {
    private Middle parent;
    
    BottomImpl() {
    }

    @Override
    public void knit(Middle middle) {
        this.parent = middle;
    }

    @Override
    @Tracks({"bottom.self","true"})
    public void callSelf() {
        doNothing();
    }

    @Override
    @Tracks({"bottom.parent","true"})
    public void callParent() {
        parent.doNothing();
    }

    @Override
    @Tracks({"bottom.nothing","true"})
    public void doNothing() {
        this.hashCode();
    }

}
