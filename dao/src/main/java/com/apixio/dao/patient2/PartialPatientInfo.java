package com.apixio.dao.patient2;

public class PartialPatientInfo
{
    public PartialPatientInfo(String partialPatientKeyHash, String documentIdHash, long time)
    {
        this.partialPatientKeyHash = partialPatientKeyHash;
        this.documentIdHash = documentIdHash;
        this.time = time;
    }

    public PartialPatientInfo(String partialPatientKeyHash, String documentIdHash, long time, String documentHash, String columnFamily)
    {
        this.partialPatientKeyHash = partialPatientKeyHash;
        this.documentIdHash = documentIdHash;
        this.time = time;
        this.documentHash = documentHash;
        this.columnFamily = columnFamily;
    }

    public String partialPatientKeyHash;
    public String documentIdHash;
    public long   time;

    public String documentHash;
    public String columnFamily;

    @Override
    public String toString()
    {
        return ("[PartialPatientInfo: "+
                "partialPatientKeyHash=" + partialPatientKeyHash +
                "; documentIdHash=" + documentIdHash +
                "; time=" + time +
                "; documentHash=" + documentHash +
                "; columnFamily=" + columnFamily +
                "]");
    }
}
