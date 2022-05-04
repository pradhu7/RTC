package apixio.cardinalsystem.api.model.cassandra.fileinfo;

import com.datastax.driver.mapping.annotations.ClusteringColumn;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

@Table(
        keyspace = "cardinal",
        name = "fileinfo_by_checksum",
        readConsistency = "QUORUM",
        writeConsistency = "QUORUM",
        caseSensitiveKeyspace = false,
        caseSensitiveTable = false
)
public class FileInfoByChecksum {

    @PartitionKey
    @Column(name = "file_checksum")
    private String fileChecksum;
    @Column(name = "last_modified_timestamp")
    private Long lastModifiedTimestamp;
    @ClusteringColumn
    @Column(name = "fileinfo_xuuid")
    private String fileinfoXuuid;

    public String getFileChecksum() {
        return fileChecksum;
    }

    public void setFileChecksum(String fileChecksum) {
        this.fileChecksum = fileChecksum;
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
