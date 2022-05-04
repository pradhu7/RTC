package apixio.cardinalsystem.api.model.cassandra.transferevent;

import com.datastax.driver.mapping.annotations.ClusteringColumn;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

@Table(
        keyspace = "cardinal",
        name = "transferevent_by_content_xuuid",
        readConsistency = "QUORUM",
        writeConsistency = "QUORUM",
        caseSensitiveKeyspace = false,
        caseSensitiveTable = false
)
public class TransferEventByContentXuuid {
    @PartitionKey
    @Column(name = "content_xuuid")
    private String contentXuuid;
    @ClusteringColumn
    @Column(name = "event_xuuid")
    private String eventXuuid;
    @ClusteringColumn(1)
    @Column(name = "to_xuuid")
    private String toXuuid;
    @ClusteringColumn(2)
    @Column(name = "from_xuuid")
    private String fromXuuid;
    @Column(name = "timestamp")
    private Long timestamp;


    public String getToXuuid() {
        return toXuuid;
    }

    public void setToXuuid(String toXuuid) {
        this.toXuuid = toXuuid;
    }

    public String getFromXuuid() {
        return fromXuuid;
    }

    public void setFromXuuid(String fromXuuid) {
        this.fromXuuid = fromXuuid;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getEventXuuid() {
        return eventXuuid;
    }

    public void setEventXuuid(String eventXuuid) {
        this.eventXuuid = eventXuuid;
    }

    public String getContentXuuid() {
        return contentXuuid;
    }

    public void setContentXuuid(String contentXuuid) {
        this.contentXuuid = contentXuuid;
    }
}
