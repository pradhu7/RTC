package com.apixio.model.nassembly;

import java.util.Map;

public class TransformationMeta
{
    private final String              functionName;
    private final Map<String, Object> inputArgs;
    private final Map<String, Object> outputArgs;

    public TransformationMeta(String functionName, Map<String, Object> inputArgs, Map<String, Object> outputArgs)
    {
        this.functionName = functionName;
        this.inputArgs    = inputArgs;
        this.outputArgs   = outputArgs;
    }

    public String getFunctionName()
    {
        return this.functionName;
    }

    public Map<String, Object> getInputArgs()
    {
        return this.inputArgs;
    }

    public Map<String, Object> getOutputArgs()
    {
        return this.outputArgs;
    }


    public Object getInputArg(String key)
    {
        return this.inputArgs.get(key);
    }

    public Object getOutputArg(String key)
    {
        return this.outputArgs.get(key);
    }
}
