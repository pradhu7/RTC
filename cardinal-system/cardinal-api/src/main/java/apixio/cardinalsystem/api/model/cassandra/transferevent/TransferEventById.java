package apixio.cardinalsystem.api.model.cassandra.transferevent;

import com.datastax.driver.mapping.annotations.ClusteringColumn;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

import java.util.Map;

@Table(
        keyspace = "cardinal",
        name = "transferevent_by_id",
        readConsistency = "QUORUM",
        writeConsistency = "QUORUM",
        caseSensitiveKeyspace = false,
        caseSensitiveTable = false
)
public class TransferEventById {
    @PartitionKey
    @Column(name = "event_xuuid")
    private String eventXuuid;
    @ClusteringColumn(0)
    @Column(name = "org_xuuid")
    private String orgXuuid;
    @Column(name = "last_modified_timestamp")
    private Long lastModifiedTimestamp;
    @ClusteringColumn(1)
    @Column(name = "status")
    private String status;
    @Column(name = "status_detailed")
    private String statusDetailed;
    @ClusteringColumn(2)
    @Column(name = "from_xuuid")
    private String fromXuuid;
    @ClusteringColumn(3)
    @Column(name = "to_xuuid")
    private String toXuuid;
    @Column(name = "event_metadata")
    private Map<String, String> eventMetadata;
    @Column(name = "content_xuuid")
    private String contentXuuid;


    public String getEventXuuid() {
        return eventXuuid;
    }

    public void setEventXuuid(String eventXuuid) {
        this.eventXuuid = eventXuuid;
    }

    public String getOrgXuuid() {
        return orgXuuid;
    }

    public void setOrgXuuid(String orgXuuid) {
        this.orgXuuid = orgXuuid;
    }

    public Long getLastModifiedTimestamp() {
        return lastModifiedTimestamp;
    }

    public void setLastModifiedTimestamp(Long lastModifiedTimestamp) {
        this.lastModifiedTimestamp = lastModifiedTimestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusDetailed() {
        return statusDetailed;
    }

    public void setStatusDetailed(String statusDetailed) {
        this.statusDetailed = statusDetailed;
    }

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

    public Map<String, String> getEventMetadata() {
        return eventMetadata;
    }

    public void setEventMetadata(Map<String, String> eventMetadata) {
        this.eventMetadata = eventMetadata;
    }

    public String getContentXuuid() {
        return contentXuuid;
    }

    public void setContentXuuid(String contentXuuid) {
        this.contentXuuid = contentXuuid;
    }
}
