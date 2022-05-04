package apixio.cardinalsystem.api.model.cassandra.transferevent;

import com.datastax.driver.mapping.annotations.ClusteringColumn;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

@Table(
        keyspace = "cardinal",
        name = "transferevent_by_to_xuuid",
        readConsistency = "QUORUM",
        writeConsistency = "QUORUM",
        caseSensitiveKeyspace = false,
        caseSensitiveTable = false
)
public class TransferEventToXuuid {
    @PartitionKey
    @Column(name = "to_xuuid")
    private String toXuuid;
    @ClusteringColumn
    @Column(name = "from_xuuid")
    private String fromXuuid;
    @Column(name = "timestamp")
    private Long timestamp;
    @Column(name = "event_xuuid")
    private String eventXuuid;

    public String getFromXuuid() {
        return fromXuuid;
    }

    public void setFromXuuid(String fromXuuid) {
        this.fromXuuid = fromXuuid;
    }

    public String getToXuuid() {
        return toXuuid;
    }

    public void setToXuuid(String toXuuid) {
        this.toXuuid = toXuuid;
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
}
