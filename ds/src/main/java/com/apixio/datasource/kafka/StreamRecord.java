package com.apixio.datasource.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;

/**
 * @author slydon
 */
public final class StreamRecord<K, V> {
    private StreamRecordMetadata metadata;
    private K key;
    private V value;

    /**
     * Creates a Stream record, this is primary external constructor.
     * @param topic The topic the record is on or will be written to.
     * @param partition The partition the record is on or will be written to (can be null).
     * @param key The key for the record.
     * @param value The record contents.
     */
    public StreamRecord(String topic, Integer partition, K key, V value) {
        this.metadata = new StreamRecordMetadata(topic, partition, null);
        this.key = key;
        this.value = value;
    }

    /**
     * Creates a record from a kafka ConsumerRecord, added as a convienance for internal use.
     */
    public StreamRecord(ConsumerRecord<K, V> record) throws Exception {
        this.metadata = new StreamRecordMetadata(record.topic(), record.partition(), record.offset());
        this.key = record.key();
        this.value = record.value();
    }

    /**
     * Creates a complete record, added for completeness.
     */
    public StreamRecord(StreamRecordMetadata metadata, K key, V value) {
        this.metadata = metadata;
        this.key = key;
        this.value = value;
    }

    public StreamRecord() {
    }

    /**
     * The metadata for this record.
     */
    public StreamRecordMetadata metadata() {
        return this.metadata;
    }

    /**
     * The key (or null if no key is specified) for this record.
     */
    public K key() {
        return this.key;
    }

    /**
     * The value for this record.
     */
    public V value() {
        return this.value;
    }

    @Override
    public String toString() {
        String key = this.key == null ? "null" : this.key.toString();
        String value = this.value == null ? "null" : this.value.toString();
        return "StreamRecord(" + metadata.toString() + ", key=" + key + ", value=" + value + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        else if (!(o instanceof StreamRecord))
            return false;

        StreamRecord<?, ?> that = (StreamRecord<?, ?>) o;

        if (!this.metadata.equals(that.metadata)) 
            return false;
        else if (this.key != null ? !this.key.equals(that.key) : that.key != null)
            return false;
        else if (this.value != null ? !this.value.equals(that.key) : that.value != null)
            return false;

        return true;
    }

    public boolean lessThan(StreamRecord s) {
        return this.metadata.topic() == s.metadata().topic() &&
               this.metadata.partition() == s.metadata().partition() &&
               this.metadata.offset() < s.metadata().offset();
    }

    public boolean greaterThan(StreamRecord s) {
        return this.metadata.topic() == s.metadata().topic() &&
               this.metadata.partition() == s.metadata().partition() &&
               this.metadata.offset() > s.metadata().offset();
    }

    @Override
    public int hashCode() {
        int result = this.metadata.hashCode();
        result = 31 * result + (key != null ? key.hashCode() : 0);
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }

    public StreamRecordMetadata getMetadata() { return metadata(); }
    public K getKey() { return key(); }
    public V getValue() { return value(); }
    public void setMetadata(StreamRecordMetadata metadata) { this.metadata = metadata; }
    public void setKey(K key) { this.key = key; }
    public void setValue(V value) { this.value = value; }
}
