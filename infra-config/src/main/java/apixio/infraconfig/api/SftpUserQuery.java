package apixio.infraconfig.api;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.Serializable;

public class SftpUserQuery implements Serializable {
    private String sftpServerId;
    private String sftpUserName;

    public SftpUserQuery(String sftpServerId, String sftpUserName) {
        this.sftpServerId = sftpServerId;
        this.sftpUserName = sftpUserName;
    }

    public String getSftpServerId() {
        return sftpServerId;
    }

    public void setSftpServerId(String sftpServerId) {
        this.sftpServerId = sftpServerId;
    }

    public String getSftpUserName() {
        return sftpUserName;
    }

    public void setSftpUserName(String sftpUserName) {
        this.sftpUserName = sftpUserName;
    }

    public String getVaultPath() {
        return String.format("secret/sftp/%s/%s", sftpServerId, sftpUserName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        SftpUserQuery that = (SftpUserQuery) o;

        return new EqualsBuilder().append(sftpServerId, that.sftpServerId).append(sftpUserName, that.sftpUserName).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(sftpServerId).append(sftpUserName).toHashCode();
    }
}
