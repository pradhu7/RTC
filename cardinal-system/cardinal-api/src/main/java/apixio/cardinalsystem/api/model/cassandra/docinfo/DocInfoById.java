package apixio.cardinalsystem.api.model.cassandra.docinfo;

import com.datastax.driver.mapping.annotations.ClusteringColumn;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

@Table(
        keyspace = "cardinal",
        name = "docinfo_by_id",
        readConsistency = "QUORUM",
        writeConsistency = "QUORUM",
        caseSensitiveKeyspace = false,
        caseSensitiveTable = false
)
public class DocInfoById {
    @PartitionKey
    @Column(name = "docnfo_xuuid")
    private String docinfoXuuid;
    @ClusteringColumn
    @Column(name = "org_xuuid")
    private String orgXuuid;
    @Column(name = "last_modified_timestamp")
    private Long lastModifiedTimestamp;
    @Column(name = "doc_checksum")
    private String docChecksum;
    @Column(name = "pds_xuuid")
    private String pdsXuuid;
    @Column(name = "patient_id")
    private String patientId;
    @Column(name = "content_type")
    private String contentType;

    public String getDocinfoXuuid() {
        return docinfoXuuid;
    }

    public void setDocinfoXuuid(String docinfoXuuid) {
        this.docinfoXuuid = docinfoXuuid;
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

    public String getDocChecksum() {
        return docChecksum;
    }

    public void setDocChecksum(String docChecksum) {
        this.docChecksum = docChecksum;
    }

    public String getPdsXuuid() {
        return pdsXuuid;
    }

    public void setPdsXuuid(String pdsXuuid) {
        this.pdsXuuid = pdsXuuid;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
}
