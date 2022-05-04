package com.apixio.datasource.kafka;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.kafka.clients.producer.RecordMetadata;

/**
 * The future result of a record's metadata (wrapping an existing future).
 */
/**
 * @author slydon
 */
public final class FutureStreamRecordMetadata implements Future<StreamRecordMetadata> {

    private final Future<RecordMetadata> future;

    public FutureStreamRecordMetadata(Future<RecordMetadata> future) {
        this.future = future;
    }

    @Override
    public boolean cancel(boolean interrupt) {
        return this.future.cancel(interrupt);
    }

    @Override
    public StreamRecordMetadata get() throws InterruptedException, ExecutionException {
        return new StreamRecordMetadata(this.future.get());
    }

    @Override
    public StreamRecordMetadata get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return new StreamRecordMetadata(this.future.get(timeout, unit));
    }

    @Override
    public boolean isCancelled() {
        return this.future.isCancelled();
    }

    @Override
    public boolean isDone() {
        return this.future.isDone();
    }
}
