package apixio.cardinalsystem.api.model.cassandra.docinfo;

import com.datastax.driver.mapping.annotations.ClusteringColumn;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

@Table(
        keyspace = "cardinal",
        name = "docinfo_by_org_pds",
        readConsistency = "QUORUM",
        writeConsistency = "QUORUM",
        caseSensitiveKeyspace = false,
        caseSensitiveTable = false
)
public class DocInfoByOrgPds {
    @PartitionKey
    @Column(name = "org_xuuid")
    private String orgXuuid;
    @PartitionKey(1)
    @Column(name = "partition_id")
    private int partitionId;
    @PartitionKey(2)
    @Column(name = "pds_id")
    private String pdsId;
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

    public String getPdsId() {
        return pdsId;
    }

    public void setPdsId(String pdsId) {
        this.pdsId = pdsId;
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
