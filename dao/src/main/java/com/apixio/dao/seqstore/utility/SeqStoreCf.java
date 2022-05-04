package com.apixio.dao.seqstore.utility;

public class SeqStoreCf
{
    public static SeqStoreCf newSeqStoreCF(String tag, String decoratedCF)
    {
        String[] split = decoratedCF.split("::");

        String   cf                 = split[0];
        long     submitTime         = (split.length >=2) ? Long.valueOf(split[1]) : 0;
        boolean  hasData            = (split.length >=3) ? Boolean.valueOf(split[2]) : true;
        boolean  migrationCompleted = (split.length ==4) ? Boolean.valueOf(split[3]) : false;

        return new SeqStoreCf(tag, cf, submitTime, hasData, migrationCompleted);
    }

    public static String decorateCF(String cf)
    {
        return cf + "::" + System.currentTimeMillis() + "::" + "false" + "::" + "false";
    }

    public String decorateCF()
    {
        return cf + "::" + submitTime + "::" + hasData;
    }

    public SeqStoreCf(String tag, String cf, long submitTime, boolean hasData, boolean migrationCompleted)
    {
        this.tag                = tag;
        this.cf                 = cf;
        this.submitTime         = submitTime;
        this.hasData            = hasData;
        this.migrationCompleted = migrationCompleted;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;

        if (obj == null)
            return false;

        if (getClass() != obj.getClass())
            return false;

        SeqStoreCf sscf = (SeqStoreCf) obj;

        return (tag.equals(sscf.tag) && cf.equals(sscf.cf) && submitTime == sscf.submitTime &&
                hasData == sscf.hasData && migrationCompleted == sscf.migrationCompleted);
    }

    @Override
    public int hashCode()
    {
        int hashCode = 1;

        hashCode = 31 * hashCode + tag.hashCode();
        hashCode = 31 * hashCode + cf.hashCode();

        return hashCode;
    }

    public String  tag;
    public String  cf;
    public long    submitTime;
    public boolean hasData;
    public boolean migrationCompleted;

    @Override
    public String toString() {
        return "SeqStoreCf{" +
                "tag='" + tag + '\'' +
                ", cf='" + cf + '\'' +
                ", submitTime=" + submitTime +
                ", hasData=" + hasData +
                ", migrationCompleted=" + migrationCompleted +
                '}';
    }
}
