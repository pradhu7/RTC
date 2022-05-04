package com.apixio.model.nassembly;

import java.io.Serializable;

/**
 * An assembly context is an interface/class that is available for all graphs/jobs
 */
public interface AssemblyContext extends Serializable
{
    String pdsId();
    String batchId();
    String runId();
    String codeVersion();
    long   timestamp();
}

