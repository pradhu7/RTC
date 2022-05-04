package com.apixio.datasource.cassandra;

import com.datastax.driver.core.ColumnDefinitions;
import com.datastax.driver.core.DataType;
import com.datastax.driver.core.PagingIterable;
import com.datastax.driver.core.Row;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class CassandraResult implements Iterable
{
    final UUID queryTraceID;
    final PagingIterable pagingIterable;

    public static class CassandraResultIterator implements Iterator
    {
        final Iterator<Row> iterator;

        public CassandraResultIterator(Iterator<Row> iterator)
        {
            this.iterator = iterator;
        }

        @Override
        public boolean hasNext()
        {
            return iterator.hasNext();
        }

        @Override
        public Map<String, Object> next()
        {
            Row row = iterator.next();

            Map<String, Object> r = new LinkedHashMap<>();
            for(ColumnDefinitions.Definition columnDefinition: row.getColumnDefinitions())
            {
                String name = columnDefinition.getName();
                DataType type = columnDefinition.getType();

                if(type == DataType.blob()) {
                    r.put(name, row.getBytes(name));
                }
                else if(type == DataType.varchar())
                {
                    r.put(name, row.getString(name));
                }
                else if(type == DataType.bigint())
                {
                    r.put(name, row.getLong(name));
                }
                else
                {
                    throw new RuntimeException(name + " has unsupported type [" + type.toString() + "]");
                }
            }

            return r;
        }

        @Override
        public void remove()
        {
            //not supported
        }
    }

    public CassandraResult(UUID queryTraceID, PagingIterable pagingIterable)
    {
        this.queryTraceID = queryTraceID;
        this.pagingIterable = pagingIterable;
    }

    @Override
    public CassandraResultIterator iterator()
    {
        return new CassandraResultIterator(pagingIterable.iterator());
    }
}
