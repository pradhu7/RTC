package com.apixio.dao.utility.sql;

import java.util.List;
import java.util.Iterator;

public class SqlOffsetIterator<T> implements Iterator<List<T>> {

    private int currentOffset;
    private final int pagination;
    private List<T> buffer;
    private boolean reachedEnd;

    private final ExecutableOffsetQuery<T> executableOffsetQuery;

    public SqlOffsetIterator(int pagination, ExecutableOffsetQuery<T> eq) {
        this(pagination, 0, eq);
    }

    public SqlOffsetIterator(int pagination, int startPage, ExecutableOffsetQuery<T> eq) {
        this.pagination = pagination;
        this.executableOffsetQuery = eq;
        this.reachedEnd = false;
        this.currentOffset = startPage;

        //Init for first call of hasNext and next
        updateBuffer();
    }

    public synchronized boolean hasNext() {
        return !reachedEnd;
    }


    public synchronized List<T> next() {
        List<T> values = this.buffer;
        if (values.size() < this.pagination)
            this.reachedEnd = true;
        else {
            updateBuffer();
        }
        return values;
    }


    private void updateBuffer(){
        this.buffer =  this.executableOffsetQuery.executeQuery(this.currentOffset, this.pagination);
        this.currentOffset += 1;
        if (this.buffer.isEmpty())
            this.reachedEnd = true;
    }
}

