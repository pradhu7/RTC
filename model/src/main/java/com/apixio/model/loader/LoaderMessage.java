package com.apixio.model.loader;

import com.apixio.model.cdi.BatchStatus;
import com.fasterxml.jackson.annotation.JsonProperty;

public class LoaderMessage
{
    @JsonProperty("time")
    private String time;

    @JsonProperty("batchName")
    private String batchName;

    @JsonProperty("batchStatus")
    private BatchStatus batchStatus;

    @JsonProperty("messageType")
    private String messageType;

    @JsonProperty("state")
    private String state;

    @JsonProperty("destination")
    private String destination;

    @JsonProperty("count")
    private int count;

    @JsonProperty("message")
    private String message;

    public LoaderMessage() {
    }

    public LoaderMessage(String time, String batchName, BatchStatus batchStatus, String messageType, String state, String destination, int count, String message)
    {
        this.time = time;
        this.batchName = batchName;
        this.batchStatus = batchStatus;
        this.messageType = messageType;
        this.state = state;
        this.destination = destination;
        this.count = count;
        this.message = message;
    }

    public String getTime()
    {
        return time;
    }

    public void setTime(String time)
    {
        this.time = time;
    }

    public String getBatchName()
    {
        return batchName;
    }

    public void setBatchName(String batchName)
    {
        this.batchName = batchName;
    }

    public BatchStatus getBatchStatus()
    {
        return batchStatus;
    }

    public void setBatchStatus(BatchStatus batchStatus)
    {
        this.batchStatus = batchStatus;
    }

    public String getMessageType()
    {
        return messageType;
    }

    public void setMessageType(String messageType)
    {
        this.messageType = messageType;
    }

    public String getState()
    {
        return state;
    }

    public void setState(String state)
    {
        this.state = state;
    }

    public String getDestination()
    {
        return destination;
    }

    public void setDestination(String destination)
    {
        this.destination = destination;
    }

    public int getCount()
    {
        return count;
    }

    public void setCount(int count)
    {
        this.count = count;
    }

    public String getMessage()
    {
        return message;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }

    public String toString()
    {
        return "LoaderMessage{" +
               "time='" + time + '\'' +
               ", batchName='" + batchName + '\'' +
               ", batchStatus=" + batchStatus +
               ", messageType='" + messageType + '\'' +
               ", state='" + state + '\'' +
               ", destination='" + destination + '\'' +
               ", count=" + count +
               ", message='" + message + '\'' +
               '}';
    }
}
