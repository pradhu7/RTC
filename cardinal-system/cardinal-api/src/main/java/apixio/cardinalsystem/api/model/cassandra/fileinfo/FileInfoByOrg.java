package apixio.cardinalsystem.api.model.cassandra.fileinfo;

import com.datastax.driver.mapping.annotations.ClusteringColumn;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

@Table(
        keyspace = "cardinal",
        name = "fileinfo_by_org",
        readConsistency = "QUORUM",
        writeConsistency = "QUORUM",
        caseSensitiveKeyspace = false,
        caseSensitiveTable = false
)
public class FileInfoByOrg {
    @PartitionKey
    @Column(name = "org_xuuid")
    private String orgXuuid;
    @PartitionKey(1)
    @Column(name = "partition_id")
    private int partitionId;
    @Column(name = "last_modified_timestamp")
    private Long lastModifiedTimestamp;
    @ClusteringColumn(0)
    @Column(name = "fileinfo_xuuid")
    private String fileinfoXuuid;

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

    public Long getLastModifiedTimestamp() {
        return lastModifiedTimestamp;
    }

    public void setLastModifiedTimestamp(Long lastModifiedTimestamp) {
        this.lastModifiedTimestamp = lastModifiedTimestamp;
    }

    public String getFileinfoXuuid() {
        return fileinfoXuuid;
    }

    public void setFileinfoXuuid(String fileinfoXuuid) {
        this.fileinfoXuuid = fileinfoXuuid;
    }
}
