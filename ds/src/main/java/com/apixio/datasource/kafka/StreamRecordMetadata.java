package com.apixio.datasource.kafka;

import org.apache.kafka.clients.producer.RecordMetadata;

/**
 * The metadata for a record in a stream.
 */
/**
 * @author slydon
 */
public final class StreamRecordMetadata {
    private String topic;
    private Integer partition;
    private Long offset;

    public StreamRecordMetadata(RecordMetadata metadata) {
        this.topic = metadata.topic();
        this.partition = metadata.partition();
        this.offset = metadata.offset();
    }

    public StreamRecordMetadata(String topic, Integer partition, Long offset) {
        this.topic = topic;
        this.partition = partition;
        this.offset = offset;
    }

    public StreamRecordMetadata() {
    }

    /**
     * The offset of the record.
     */
    public Long offset() {
        return this.offset;
    }

    /**
     * The topic of the record.
     */
    public String topic() {
        return this.topic;
    }

    /**
     * The partition of the record.
     */
    public Integer partition() {
        return this.partition;
    }

    @Override
    public String toString() {
        return "StreamRecordMetadata(topic=" + topic + ", partition=" + partition + ", offset=" + offset + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        else if (!(o instanceof StreamRecordMetadata))
            return false;

        StreamRecordMetadata that = (StreamRecordMetadata) o;

        if (that.topic == null || !this.topic.equals(that.topic))
            return false;
        else if (this.partition != null ? !this.partition.equals(that.partition) : that.partition != null)
            return false;
        else if (this.offset != null ? !this.offset.equals(that.offset) : that.offset != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = topic != null ? topic.hashCode() : 0;
        result = 31 * result + (partition != null ? partition.hashCode() : 0);
        result = 31 * result + (offset != null ? offset.hashCode() : 0);
        return result;
    }

    public long getOffset() { return offset(); }
    public String getTopic() { return topic(); }
    public int getPartition() { return partition(); }
    public void setOffset(Long offset) { this.offset = offset; }
    public void setTopic(String topic) { this.topic = topic; }
    public void setPartition(Integer partition) { this.partition = partition; }
}
