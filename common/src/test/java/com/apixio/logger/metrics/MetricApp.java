package com.apixio.logger.metrics;

public interface MetricApp {
    
    public void timer();
    
    public void meter();
    
    public void exception(boolean toss) throws Exception;

}
