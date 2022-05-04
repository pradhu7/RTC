package apixio.cardinalsystem.api.model.cassandra.docinfo;

import com.datastax.driver.mapping.annotations.ClusteringColumn;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

@Table(
        keyspace = "cardinal",
        name = "docinfo_by_org_patient",
        readConsistency = "QUORUM",
        writeConsistency = "QUORUM",
        caseSensitiveKeyspace = false,
        caseSensitiveTable = false
)
public class DocInfoByOrgPatientId {

    @PartitionKey
    @Column(name = "org_xuuid")
    private String orgXuuid;
    @PartitionKey(1)
    @Column(name = "partition_id")
    private int partitionId;
    @PartitionKey(2)
    @Column(name = "patient_id")
    private String patientId;
    @Column(name = "last_modified_timestamp")
    private Long lastModifiedTimestamp;
    @ClusteringColumn
    @Column(name = "docnfo_xuuid")
    private String docinfoXuuid;

    public String getOrgXuuid() {
        return orgXuuid;
    }

    public void setOrgXuuid(String orgXuuid) {
        this.orgXuuid = orgXuuid;
    }

    public int getPartitionId() {
        return partitionId;
    }

    public void setPartitionId(int partitionId) {
        this.partitionId = partitionId;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public Long getLastModifiedTimestamp() {
        return lastModifiedTimestamp;
    }

    public void setLastModifiedTimestamp(Long lastModifiedTimestamp) {
        this.lastModifiedTimestamp = lastModifiedTimestamp;
    }

    public String getDocinfoXuuid() {
        return docinfoXuuid;
    }

    public void setDocinfoXuuid(String docinfoXuuid) {
        this.docinfoXuuid = docinfoXuuid;
    }
}
