package com.apixio.datasource.cassandra;

import com.apixio.datasource.cassandra.field.ByteBufferField;
import com.apixio.datasource.cassandra.field.Field;
import com.apixio.datasource.cassandra.field.LongField;
import com.apixio.datasource.cassandra.field.StringField;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by dyee on 3/8/18.
 */
public class StatementGenerator
{
    final static String ROWKEY_STRING = "rowkey";

    public enum CREATE_TYPE
    {
        LEVELED(" CREATE TABLE IF NOT EXISTS %s ( " +
                " %s," +
                " PRIMARY KEY (%s) " +
                " ) WITH " +
                " comment = 'table with dynamic columns' AND " +
                " caching = 'KEYS_ONLY' AND " +
                " bloom_filter_fp_chance = 0.1 AND " +
                " read_repair_chance = 0.0 AND " +
                " dclocal_read_repair_chance = 0.1 AND " +
                " gc_grace_seconds = 864000 AND " +
                " compaction = { " +
                " 'class' :  'LeveledCompactionStrategy', " +
                " 'sstable_size_in_mb' : 160,  'tombstone_compaction_interval' : 864000, 'tombstone_threshold' : 0.2   " +
                " } AND " +
                " compression = {'sstable_compression' : 'LZ4Compressor', 'chunk_length_kb' : 4}; "),
        SIZE_TIERED(" CREATE TABLE IF NOT EXISTS %s ( " +
                " %s, " +
                " PRIMARY KEY (%s) " +
                " ) WITH " +
                " comment = 'table with dynamic columns' AND " +
                " caching = 'KEYS_ONLY' AND " +
                " bloom_filter_fp_chance = 0.1 AND " +
                " read_repair_chance = 0.0 AND " +
                " dclocal_read_repair_chance = 0.1 AND " +
                " gc_grace_seconds = 864000 AND " +
                " compaction = { " +
                " 'class' :  'SizeTieredCompactionStrategy', " +
                " 'tombstone_compaction_interval' : 864000, 'tombstone_threshold' : 0.2   " +
                " } AND " +
                " compression = {'sstable_compression' : 'LZ4Compressor', 'chunk_length_kb' : 4}; ");

        String createStatement;

        CREATE_TYPE(String type)
        {
            createStatement = type;
        }

        public String getCreateStatement()
        {
            return createStatement;
        }
    }

    final FieldList fieldList;

    public StatementGenerator(String ... fields)
    {
        this.fieldList = new FieldList(Arrays.asList(fields));
    }

    StatementGenerator(FieldList fieldList)
    {
        this.fieldList = fieldList;
    }

    public String create(String table, FieldValueList fieldValueList, String partitionKey, String clusterKey)
    {
        return create(CREATE_TYPE.LEVELED, table, fieldValueList, partitionKey, clusterKey);
    }

    public String create(String table, FieldValueList fieldValueList, String partitionKey, List<String> clusterKeys)
    {
        return create(CREATE_TYPE.LEVELED, table, fieldValueList, partitionKey, clusterKeys);
    }

    public String create(CREATE_TYPE createType, String table, FieldValueList fieldValueList, String partitionKey, String clusterKey)
    {
        List<String>clusterKeys = new ArrayList();
        if (StringUtils.isNotBlank(clusterKey)) clusterKeys.add(clusterKey);

        return create(createType, table, fieldValueList, partitionKey, clusterKeys);
    }

    public String create(CREATE_TYPE createType, String table, FieldValueList fieldValueList, String partitionKey, List<String> clusterKeys)
    {
        //
        // Generate Typed Columns list
        //
        StringBuilder typedColumns = new StringBuilder();
        if (fieldValueList != null)
        {
            for (Map.Entry<Field, Object> fieldValue : fieldValueList.getFieldValue().entrySet())
            {
                switch (fieldValue.getKey().getType())
                {
                    case LongField.TYPE:
                        typedColumns.append((typedColumns.length() == 0) ? "" : ", ");
                        typedColumns.append(fieldValue.getKey().getName()).append(" bigint");
                        break;
                    case StringField.TYPE:
                        typedColumns.append((typedColumns.length() == 0) ? "" : ", ");
                        typedColumns.append(fieldValue.getKey().getName()).append(" text");
                        break;
                    case ByteBufferField.TYPE:
                        typedColumns.append((typedColumns.length() == 0) ? "" : ", ");
                        typedColumns.append(fieldValue.getKey().getName()).append(" blob");
                        break;
                    default:
                        throw new RuntimeException("Error: [" + fieldValue.getKey().getType() + " is not a supported Type");
                }
            }
        }

        if (clusterKeys.isEmpty())
        {
            return String.format(createType.getCreateStatement(), table, typedColumns.toString(), partitionKey);
        }
        else
        {
            String clusterKeysStr = clusterKeys.stream().collect(Collectors.joining(",", "", ""));
            return String.format(createType.getCreateStatement(), table, typedColumns.toString(), partitionKey + " , " + clusterKeysStr);
        }
    }

    public String select(String table, FieldValueList fieldValueList)
    {
        StringBuilder whereClause = new StringBuilder();
        if (fieldValueList != null)
        {
            for (Map.Entry<Field, Object> fieldValue : fieldValueList.getFieldValue().entrySet())
            {
                switch (fieldValue.getKey().getType())
                {
                    case LongField.TYPE:
                        whereClause.append((whereClause.length() == 0) ? " where " : " AND  ");
                        whereClause.append(fieldValue.getKey().getName()).append(" = ? ");
                        break;
                    case StringField.TYPE:
                        whereClause.append((whereClause.length() == 0) ? " where " : " AND  ");
                        whereClause.append(fieldValue.getKey().getName()).append(" = ? ");
                        break;
                    case ByteBufferField.TYPE:
                        //ignore, this is the actual value
                        break;
                    default:
                        throw new RuntimeException("Error: [" + fieldValue.getKey().getType() + " is not a supported Type");
                }
            }
        }

        if (StringUtils.isNotBlank(table))
        {
            return "SELECT " + fieldList.mkString() + " FROM " + table + whereClause;
        } else
        {
            throw new RuntimeException("Error: table is blank");
        }
    }

    public FieldValueList getQueryFieldValueList(FieldValueList fieldValueList)
    {
        QueryFieldValueListBuilder qfvlBuilder = new QueryFieldValueListBuilder();

        if (fieldValueList != null)
        {
            for (Map.Entry<Field, Object> fieldValue : fieldValueList.getFieldValue().entrySet())
            {
                switch (fieldValue.getKey().getType())
                {
                    case LongField.TYPE:
                        qfvlBuilder.addLong(fieldValue.getKey().getName(), (Long)fieldValue.getValue());
                        break;
                    case StringField.TYPE:
                        qfvlBuilder.addString(fieldValue.getKey().getName(), (String)fieldValue.getValue());
                        break;
                    case ByteBufferField.TYPE:
                        //ignore, this is the actual value
                        break;
                    default:
                        throw new RuntimeException("Error: [" + fieldValue.getKey().getType() + " is not a supported Type");
                }
            }
        }
        return qfvlBuilder.build();
    }

    public String insert(String table, long ttl)
    {
        return insert(table, false, ttl);
    }

    public String insert(String table, boolean ifNotExists, long ttl)
    {
        if (StringUtils.isNotBlank(table))
        {
            return "INSERT INTO " + table +
                    " (" + fieldList.mkString() + ") VALUES (" +
                    fieldList.mkPlaceHolders() + ")" + (ifNotExists ? " if not exists" : "") + (ttl > 0L  ? " using ttl "  + ttl : "");
        }
        else
        {
            throw new RuntimeException("Error: table is blank");
        }
    }

    public String deleteRow(String table, FieldValueList fieldValueList)
    {
        StringBuilder whereClause = new StringBuilder();
        if (fieldValueList != null)
        {
            for (Map.Entry<Field, Object> fieldValue : fieldValueList.getFieldValue().entrySet())
            {
                switch (fieldValue.getKey().getType())
                {
                    case StringField.TYPE:
                        if(fieldValue.getKey().getName().equals(ROWKEY_STRING))
                        {
                            whereClause.append((whereClause.length() == 0) ? " where " : " AND  ");
                            whereClause.append(fieldValue.getKey().getName()).append(" = ? ");
                        }
                        break;
                    case ByteBufferField.TYPE:
                        //ignore, this is the actual value
                        break;
                    default:
                        throw new RuntimeException("Error: [" + fieldValue.getKey().getType() + " is not a supported Type");
                }
            }
        }

        if (StringUtils.isNotBlank(table))
        {
            return "DELETE FROM " + table + whereClause;
        } else
        {
            throw new RuntimeException("Error: table is blank");
        }
    }

    public String deleteColumn(String table, FieldValueList fieldValueList)
    {
        StringBuilder whereClause = new StringBuilder();
        if (fieldValueList != null)
        {
            for (Map.Entry<Field, Object> fieldValue : fieldValueList.getFieldValue().entrySet())
            {
                switch (fieldValue.getKey().getType())
                {
                    case LongField.TYPE:
                        whereClause.append((whereClause.length() == 0) ? " where " : " AND  ");
                        whereClause.append(fieldValue.getKey().getName()).append(" = ? ");
                        break;
                    case StringField.TYPE:
                        whereClause.append((whereClause.length() == 0) ? " where " : " AND  ");
                        whereClause.append(fieldValue.getKey().getName()).append(" = ? ");
                        break;
                    case ByteBufferField.TYPE:
                        //ignore, this is the actual value
                        break;
                    default:
                        throw new RuntimeException("Error: [" + fieldValue.getKey().getType() + " is not a supported Type");
                }
            }
        }

        if (StringUtils.isNotBlank(table))
        {
            return "DELETE FROM " + table + whereClause;
        } else
        {
            throw new RuntimeException("Error: table is blank");
        }
    }
}
