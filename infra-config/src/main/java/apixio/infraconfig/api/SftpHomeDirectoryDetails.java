package apixio.infraconfig.api;

import apixio.infraconfig.core.InvalidSftpUserException;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.Objects;

@JsonDeserialize(builder = SftpHomeDirectoryDetails.SftpHomeDirectoryDetailsBuilder.class)
public class SftpHomeDirectoryDetails {
    @JsonProperty("Entry")
    public String entry;
    @JsonProperty("Target")
    public String target;

    public String getEntry() {
        return entry;
    }

    public void setEntry(String entry) throws InvalidSftpUserException {
        if (Objects.isNull(entry)) {
            throw new InvalidSftpUserException("Must have entry field in homedirectorydetails");
        }
        this.entry = entry;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) throws InvalidSftpUserException {
        if (Objects.isNull(target)) {
            throw new InvalidSftpUserException("must have target field in homedirectorydetails");
        }
        this.target = target;
    }

    @JsonPOJOBuilder()
    public static final class SftpHomeDirectoryDetailsBuilder {
        @JsonProperty("Entry")
        public String entry;
        @JsonProperty("Target")
        public String target;

        private SftpHomeDirectoryDetailsBuilder() {
        }

        public static SftpHomeDirectoryDetailsBuilder builder() {
            return new SftpHomeDirectoryDetailsBuilder();
        }

        public SftpHomeDirectoryDetailsBuilder withEntry(String entry) {
            this.entry = entry;
            return this;
        }

        public SftpHomeDirectoryDetailsBuilder withTarget(String target) {
            this.target = target;
            return this;
        }

        public SftpHomeDirectoryDetails build() throws InvalidSftpUserException {
            SftpHomeDirectoryDetails sftpHomeDirectoryDetails = new SftpHomeDirectoryDetails();
            sftpHomeDirectoryDetails.setEntry(entry);
            sftpHomeDirectoryDetails.setTarget(target);
            return sftpHomeDirectoryDetails;
        }
    }
}
