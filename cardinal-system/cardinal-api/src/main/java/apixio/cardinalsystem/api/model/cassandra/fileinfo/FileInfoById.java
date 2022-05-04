package apixio.cardinalsystem.api.model.cassandra.fileinfo;

import com.datastax.driver.mapping.annotations.ClusteringColumn;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

import java.util.Map;

@Table(
        keyspace = "cardinal",
        name = "fileinfo_by_id",
        readConsistency = "QUORUM",
        writeConsistency = "QUORUM",
        caseSensitiveKeyspace = false,
        caseSensitiveTable = false
)
public class FileInfoById {
    @PartitionKey
    @Column(name = "fileinfo_xuuid")
    private String fileinfoXuuid;
    @ClusteringColumn(0)
    @Column(name = "org_xuuid")
    private String orgXuuid;
    @Column(name = "last_modified_timestamp")
    private Long lastModifiedTimestamp;
    @Column(name = "file_checksum")
    private String fileChecksum;
    @Column(name = "file_size")
    private Long fileSize;
    @Column(name = "file_path")
    private Map<String, String> filePath;
    @Column(name = "is_archive")
    private Boolean isArchive;
    @Column(name = "mime_type")
    private String mimeType;

    public String getFileinfoXuuid() {
        return fileinfoXuuid;
    }

    public void setFileinfoXuuid(String fileinfoXuuid) {
        this.fileinfoXuuid = fileinfoXuuid;
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

    public String getFileChecksum() {
        return fileChecksum;
    }

    public void setFileChecksum(String fileChecksum) {
        this.fileChecksum = fileChecksum;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public Map<String, String> getFilePath() {
        return filePath;
    }

    public void setFilePath(Map<String, String> filePath) {
        this.filePath = filePath;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public Boolean getIsArchive() {
        return isArchive;
    }

    public void setIsArchive(Boolean archive) {
        isArchive = archive;
    }
}
