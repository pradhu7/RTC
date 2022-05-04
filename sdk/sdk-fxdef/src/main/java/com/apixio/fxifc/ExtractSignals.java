package com.apixio.fxifc;

// manually generated from extractFeature.idl

import java.util.List;

import com.apixio.ensemble.ifc.Signal;
import com.apixio.ensemble.ifc.PageWindow;

public interface ExtractSignals
{
    public List<Signal> extractSignals(PageWindow pw) throws Exception;
    public void setup() throws Exception;
}
