package apixio.cardinalsystem.api.model.cassandra.processevent;

import com.datastax.driver.mapping.annotations.ClusteringColumn;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

import java.util.Map;

@Table(
        keyspace = "cardinal",
        name = "processevent_by_id",
        readConsistency = "QUORUM",
        writeConsistency = "QUORUM",
        caseSensitiveKeyspace = false,
        caseSensitiveTable = false
)
public class ProcessEventById {
    @PartitionKey(0)
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
    @Column(name = "event_type")
    private String eventType;
    @ClusteringColumn(3)
    @Column(name = "from_xuuid")
    private String fromXuuid;
    @ClusteringColumn(4)
    @Column(name = "to_xuuid")
    private String toXuuid;
    @Column(name = "event_subtype")
    private String eventSubtype;
    @Column(name = "event_metadata")
    private Map<String, String> eventMetadata;
    @Column(name = "code_version")
    private String codeVersion;
    @Column(name = "execute_host")
    private String executeHost;
    @Column(name = "execute_duration")
    private int executeDuration;

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

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
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

    public String getEventSubtype() {
        return eventSubtype;
    }

    public void setEventSubtype(String eventSubtype) {
        this.eventSubtype = eventSubtype;
    }

    public Map<String, String> getEventMetadata() {
        return eventMetadata;
    }

    public void setEventMetadata(Map<String, String> eventMetadata) {
        this.eventMetadata = eventMetadata;
    }

    public String getCodeVersion() {
        return codeVersion;
    }

    public void setCodeVersion(String codeVersion) {
        this.codeVersion = codeVersion;
    }

    public String getExecuteHost() {
        return executeHost;
    }

    public void setExecuteHost(String executeHost) {
        this.executeHost = executeHost;
    }

    public int getExecuteDuration() {
        return executeDuration;
    }

    public void setExecuteDuration(int executeDuration) {
        this.executeDuration = executeDuration;
    }
}
