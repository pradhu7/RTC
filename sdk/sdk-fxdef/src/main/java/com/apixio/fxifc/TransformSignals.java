package com.apixio.fxifc;

import java.util.List;

import com.apixio.ensemble.ifc.Signal;

public interface TransformSignals
{
    public List<Signal> transformSignals(List<Signal> signals) throws Exception;
    public void setup() throws Exception;
}
