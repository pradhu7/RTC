package apixio.cardinalsystem.api.model.cassandra.docinfo;

import com.datastax.driver.mapping.annotations.ClusteringColumn;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

@Table(
        keyspace = "cardinal",
        name = "docinfo_by_checksum",
        readConsistency = "QUORUM",
        writeConsistency = "QUORUM",
        caseSensitiveKeyspace = false,
        caseSensitiveTable = false
)
public class DocInfoByChecksum {
    @PartitionKey
    @Column(name = "doc_checksum")
    private String docChecksum;
    @Column(name = "last_modified_timestamp")
    private Long lastModifiedTimestamp;
    @ClusteringColumn
    @Column(name = "docinfo_xuuid")
    private String docinfoXuuid;


    public String getDocinfoXuuid() {
        return docinfoXuuid;
    }

    public void setDocinfoXuuid(String docinfoXuuid) {
        this.docinfoXuuid = docinfoXuuid;
    }


    public String getDocChecksum() {
        return docChecksum;
    }

    public void setDocChecksum(String docChecksum) {
        this.docChecksum = docChecksum;
    }

    public Long getLastModifiedTimestamp() {
        return lastModifiedTimestamp;
    }

    public void setLastModifiedTimestamp(Long lastModifiedTimestamp) {
        this.lastModifiedTimestamp = lastModifiedTimestamp;
    }
}
