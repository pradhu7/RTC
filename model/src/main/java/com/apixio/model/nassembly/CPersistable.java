package com.apixio.model.nassembly;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
  CREATE TABLE "application".apx_cfnassembly (
          rowkey text,
          col1s text,
          col2s text,
          col3s text,
          col1l bigint,
          col2l bigint,
          d blob,
          PRIMARY KEY ( (rowkey), col1s, col2s, col3s, col1l, col2l)
          )
          WITH CLUSTERING ORDER BY (col1s DESC, col2s DESC, col3s DESC, col1l DESC, col2l DESC)
          AND bloom_filter_fp_chance = 0.1
          AND caching = '{"keys":"ALL", "rows_per_partition":"NONE"}'
          AND comment = 'table with dynamic columns'
          AND compaction = {'sstable_size_in_mb': '160', 'tombstone_threshold': '0.2', 'tombstone_compaction_interval': '864000', 'class': 'org.apache.cassandra.db.compaction.LeveledCompactionStrategy'}
          AND compression = {'chunk_length_kb': '4', 'sstable_compression': 'org.apache.cassandra.io.compress.LZ4Compressor'}
          AND dclocal_read_repair_chance = 0.1
          AND default_time_to_live = 0
          AND gc_grace_seconds = 864000
          AND max_index_interval = 2048
          AND memtable_flush_period_in_ms = 0
          AND min_index_interval = 128
          AND read_repair_chance = 0.0
          AND speculative_retry = '99.0PERCENTILE';
  */

public interface CPersistable<T extends Exchange> extends Base
{
    String persistenceColValue = "merged";

    public class ColValueWithPersistentField
    {
        public ColValueWithPersistentField(Object value, String persistentFieldName)
        {
            this.value               = value;
            this.persistentFieldName = persistentFieldName;
        }

        public Object getValue()
        {
            return value;
        }

        public String getPersistentFieldName()
        {
            return persistentFieldName;
        }

        private Object   value;
        private String   persistentFieldName;
    }

    /**
     * The default table name
     * @return
     */
    default AssemblySchema getSchema()
    {
        // "col" is just a placeholder. "col" is the logical name of the column if you don't care about customization
        AssemblySchema.AssemblyCol col = new AssemblySchema.AssemblyCol("col", AssemblySchema.ColType.StringType);
        AssemblySchema.AssemblyCol[] cols = new AssemblySchema.AssemblyCol[]{col};

        return new AssemblySchema(cols);
    }

    /**
     * rowkey definition
     *
     * @return
     */
    default String getPersistenceRowKey(T exchange)
    {
        return exchange.getCid();
    }

    /**
     * Describes how to link between values and persistence names for clustered Columns
     *
     * @return
     */
    default List<ColValueWithPersistentField> clusteringColumnsWithValues(T exchange)
    {
        return Arrays.asList(new ColValueWithPersistentField(persistenceColValue, getSchema().getClusteringCols()[0].getName()));
    }

    /**
     * The name of the rowkey in Cassandra
     * @return
     */
    default String getPersistenceProtobufFieldName() { return getSchema().getProtobufFieldName(); }

}