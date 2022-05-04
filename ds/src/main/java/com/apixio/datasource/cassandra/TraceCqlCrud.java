package com.apixio.datasource.cassandra;

import com.apixio.datasource.cassandra.field.ByteBufferField;
import com.apixio.datasource.cassandra.field.Field;
import com.apixio.datasource.cassandra.field.LongField;
import com.apixio.datasource.cassandra.field.StringField;
import com.apixio.utility.StringUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class TraceCqlCrud
{
    private final static String TYPE = "type";
    private final static String TRACEID   = "traceID";
    private final static String OPS       = "ops";
    private final static String TABLENAME = "tableName";
    private final static String ROWKEY    = "rowkey";
    private final static String COLUMN    = "column";
    private final static String VERSION   = "version";

    private ThreadLocal<List<CqlRowData>> cqlTrace = new ThreadLocal<>();
    private boolean addTraceOps;

    public void setAddTraceOps(boolean addTraceOps)
    {
        this.addTraceOps = addTraceOps;
    }

    private static Set<String> excludeSet = new HashSet<String>();

    static {
        excludeSet.add("traceID");
        excludeSet.add("tableName");
        excludeSet.add("ops");
        excludeSet.add("version");
    }

    public void addToTrace(CqlRowData cqlRowData)
    {
        if (addTraceOps)
        {
            List<CqlRowData> all = getTrace();
            all.add(cqlRowData);
        }
    }

    @Deprecated
    public Map<String, ByteBuffer> serializeTracesOld()
    {
        Map<String, ByteBuffer> traces   = new HashMap<>();
        List<CqlRowData>        all      = getTrace();
        String                  traceID  = UUID.randomUUID().toString();

        for (CqlRowData cqlRowData: all)
        {
            Map<String, String> key = new HashMap<>();
            key.put(TRACEID,   traceID);
            key.put(OPS,       cqlRowData.ops.name());
            key.put(TABLENAME, cqlRowData.tableName);
            key.put(ROWKEY,    cqlRowData.rowkey);
            key.put(COLUMN,    cqlRowData.column);

            traces.put(StringUtil.mapToString(key), cqlRowData.value);
        }

        return traces;
    }


    public Map<String, ByteBuffer> serializeTraces()
    {
        Map<String, ByteBuffer> traces   = new HashMap<>();
        List<CqlRowData>        all      = getTrace();
        String                  traceID  = UUID.randomUUID().toString();

        for (CqlRowData cqlRowData : all)
        {
            Map<String, String> key = new HashMap<>();
            ByteBuffer value = null;

            key.put(TRACEID, traceID);
            key.put(OPS, cqlRowData.ops.name());
            key.put(TABLENAME, cqlRowData.tableName);
            key.put(COLUMN, cqlRowData.column);
            key.put(VERSION, "1.0");

            FieldValueList fieldValueList = cqlRowData.getFieldValueList();

            //serialize only String values

            for (Map.Entry<Field, Object> fieldValue : fieldValueList.getFieldValue().entrySet())
            {
                switch (fieldValue.getKey().getType())
                {
                    case LongField.TYPE:
                        key.put(fieldValue.getKey().getName(), "{java.lang.Long}_" + Long.toString((Long)fieldValue.getValue()));
                        break;
                    case StringField.TYPE:
                        key.put(fieldValue.getKey().getName(), (String) fieldValue.getValue());
                        break;
                    case ByteBufferField.TYPE:
                        value = (ByteBuffer) fieldValue.getValue();
                        break;
                    default:
                        throw new RuntimeException("found a field value of type [" + fieldValue.getKey().getName() + "], which is unsupported");
                }
            }

            if (value == null)
            {
                throw new RuntimeException("no value found in FieldValueList");
            }

            traces.put(StringUtil.mapToString(key), value);
        }

        return traces;
    }

    public TraceIdAndCqlRowData deserializeTrace(String key, ByteBuffer value)
    {
        Map<String, String> mapped = StringUtil.mapFromString(key);

        List<String> statementList = new ArrayList<>();
        statementList.add("d");
        FieldValueListBuilder fieldValueListBuilder = new FieldValueListBuilder();

        boolean hasVersion = false;
        for(String field: mapped.keySet())
        {
            if(field.equals(VERSION)) hasVersion = true;

            if(!excludeSet.contains(field))
            {
                statementList.add(field);
                try
                {
                    Object fieldValue = getValue(mapped.get(field));
                    if(fieldValue instanceof String)
                    {
                        fieldValueListBuilder.addString(field, (String)fieldValue);
                    }
                    else if (fieldValue instanceof Long)
                    {
                        fieldValueListBuilder.addLong(field, (Long)fieldValue);
                    }
                } catch (ClassNotFoundException e)
                {
                    e.printStackTrace();
                }
            }
        }

        fieldValueListBuilder.addByteBuffer("d", value);

        FieldValueList fieldValueList = fieldValueListBuilder.build();

        if(hasVersion) {
            statementList.remove("column");
            fieldValueList.getFieldValue().remove(new StringField("column"));
        }

        StatementGenerator statementGenerator = new StatementGenerator(new FieldList(statementList));

        CqlRowData cqlRowData = new CqlRowData(mapped.get(TABLENAME),statementGenerator, fieldValueList);


        cqlRowData.setCqlOps(CqlRowData.CqlOps.valueOf(mapped.get(OPS)));
        return new TraceIdAndCqlRowData(mapped.get(TRACEID), cqlRowData);
    }

    public void cleanTrace()
    {
        cqlTrace.set(null);
        cqlTrace.remove();
    }

    static public Object getValue(String fieldValue) throws ClassNotFoundException
    {
        if(!fieldValue.startsWith("{"))
            return fieldValue;

        String [] parsedFieldValue = fieldValue.split("_");
        String fieldClass = parsedFieldValue[0].substring(1, parsedFieldValue[0].length()-1);

        Object returnVal = null;
        switch (Class.forName(fieldClass).toString()) {
            case "class java.lang.Long":
                returnVal = new Long(parsedFieldValue[1]);
                break;
        }

        return returnVal;
    }

    private List<CqlRowData> getTrace()
    {
        List<CqlRowData> all = cqlTrace.get();

        if (all == null)
        {
            all = new ArrayList<>();
            cqlTrace.set(all);
        }

        return all;
    }

    public static class TraceIdAndCqlRowData
    {
        public TraceIdAndCqlRowData(String traceID, CqlRowData cqlRowData)
        {
            this.traceID = traceID;
            this.cqlRowData = cqlRowData;
        }

        public String     traceID;
        public CqlRowData cqlRowData;

    }
}
