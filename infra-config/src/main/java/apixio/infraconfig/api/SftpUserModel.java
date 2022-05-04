package apixio.infraconfig.api;

import apixio.infraconfig.core.InvalidSftpUserException;
import apixio.infraconfig.core.PasswordHashGeneratorHelper;
import apixio.infraconfig.core.SshKeyUtils;
import apixio.infraconfig.core.ValidatePassword;
import com.apixio.SysServices;
import com.apixio.XUUID;
import com.apixio.useracct.entity.Organization;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Feature;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.commons.net.util.SubnetUtils;

import java.io.IOException;
import java.io.Serializable;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * + UserName (string)
 * <p>
 * + Password (string)
 * <p>
 * + Optional: PublicKey (string)
 * <p>
 * + Optional: PrivateKey (string)
 * <p>
 * + SFTPServerID (string)
 * <p>
 * + Optional: PDS (list(string))
 * <p>
 * + AcceptedIpNetwork (list(String))
 */
@JsonInclude(Include.NON_NULL)
@JsonDeserialize(builder = SftpUserModel.SftpUserModelBuilder.class)
@JsonIgnoreProperties(value = {"GROUP_WHITELIST", "PrivateKeyRaw", "PasswordRaw", "RemovedCidrs"}, ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class SftpUserModel implements Serializable {
    public Set<String> GROUP_WHITELIST = new HashSet<>(Arrays.asList("SYSTEM", "INTERNAL"));

    @JsonProperty("Username")
    public String username;
    @JsonProperty("Password")
    public String password;
    @JsonProperty("PublicKey")
    public String publicKey;
    @JsonProperty("PrivateKey")
    public String privateKey;
    @JsonProperty("SFTPServerID")
    public String sftpServerId;
    @JsonProperty("PDS")
    public Set<String> pds;
    @JsonProperty("AcceptedIpNetwork")
    public Set<String> acceptedIpNetwork;
    @JsonProperty("Group")
    public String group;
    @JsonProperty("HomeDirectoryDetails")
    public String homeDirectoryDetails;
    @JsonProperty("HomeDirectoryType")
    public String homeDirectoryType;
    @JsonProperty("Role")
    public String awsRole;
    @JsonProperty("Active")
    public Boolean active = true;
    @JsonProperty("Initialized")
    public Boolean initialized = false;
    public String privateKeyRaw;
    public String passwordRaw;
    public Set<String> removedCidrs = new HashSet<>();

    public void setInitialized(Boolean initialized) {
        this.initialized = initialized;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getAwsRole() {
        return awsRole;
    }

    public void setAwsRole(String awsRole) {
        this.awsRole = awsRole;
    }

    public String getHomeDirectoryDetails() {
        return homeDirectoryDetails;
    }

    public void setHomeDirectoryDetails(String homeDirectoryDetails) throws InvalidSftpUserException {
        ObjectMapper oMapper = new ObjectMapper();
        if (Objects.isNull(homeDirectoryDetails)) {
            this.homeDirectoryDetails = homeDirectoryDetails;
            return;
        }
        try {
            oMapper.readValue(homeDirectoryDetails, oMapper.getTypeFactory().constructCollectionType(List.class, SftpHomeDirectoryDetails.class));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new InvalidSftpUserException(String.format("Unable to parse home directory details: %s", e.getMessage()));
        }
        this.homeDirectoryDetails = homeDirectoryDetails;
    }

    public void setHomeDirectoryDetails(List<SftpHomeDirectoryDetails> homeDirectoryDetails) throws InvalidSftpUserException {
        ObjectMapper oMapper = new ObjectMapper();
        try {
            setHomeDirectoryDetails(oMapper.writeValueAsString(homeDirectoryDetails));
        } catch (JsonProcessingException e) {
            throw new InvalidSftpUserException(String.format("Unable to serialize homeDirectoryDetails: %s", homeDirectoryDetails.toString()));
        }
    }

    public String getHomeDirectoryType() {
        return homeDirectoryType;
    }

    public void setHomeDirectoryType(String homeDirectoryType) throws InvalidSftpUserException {
        if (Objects.isNull(homeDirectoryType)) {
            homeDirectoryType = "LOGICAL";
        }
        if (!homeDirectoryType.equals("LOGICAL")) {
            throw new InvalidSftpUserException(String.format("HomeDirectoryType can only be set to LOGICAL. you set %s", homeDirectoryType));
        }
        this.homeDirectoryType = homeDirectoryType;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) throws InvalidSftpUserException {
        if (Objects.isNull(group)) {
            throw new InvalidSftpUserException("Group can not be null");
        }
        this.group = group;
    }

    // the validate method for groups is not inline since it requires an external call to validate the xuuid is a valid organization
    public Boolean validateGroup(SysServices sysServices) throws InvalidSftpUserException {
        if (GROUP_WHITELIST.contains(group)) {
            return true;
        } else {
            try {
                XUUID groupXUUID = XUUID.fromString(group);
                Organization organization = sysServices.getOrganizations().findOrganizationByID(groupXUUID);
                if (Objects.nonNull(organization)) {
                    return true;
                } else {
                    throw new InvalidSftpUserException(String.format("%s is not a valid org", group));
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new InvalidSftpUserException(e.getMessage());
            }
        }
    }

    public void setUsername(String username) throws InvalidSftpUserException {
        if (Objects.nonNull(username) && username.length() < 5) {
            throw new InvalidSftpUserException(String.format("Must have username greater than 5 characters: %s", username));
        }

        if (Objects.nonNull(username) && username.matches(".*[A-Z]+.*")) {
            throw new InvalidSftpUserException(String.format("Username may not have uppercase letters: %s", username));
        }
        this.username = username;
    }

    public void setPassword(String password) throws InvalidSftpUserException {
        if (Objects.isNull(password) || !password.startsWith(PasswordHashGeneratorHelper.hashPrefix)) {
            ValidatePassword validatePassword = new ValidatePassword();
            if (Objects.isNull(password) || password.isEmpty()) {
                password = validatePassword.generatePassword(24);
            }
            Triple<Boolean, String, String> validPassword = validatePassword.checkPassword(this.username, password);

            if (!validPassword.getLeft()) {
                throw new InvalidSftpUserException(
                        String.format("Password does not meet minimum requirements.\nFeedback:\n%s\nSuggestions: \n%s",
                                validPassword.getMiddle(),
                                validPassword.getRight()
                        )
                );
            }
            this.passwordRaw = password;
            this.password = PasswordHashGeneratorHelper.hash(password);
        } else {
            this.passwordRaw = null;
            this.password = password;
        }

    }

    public void setPublicKey(String publicKey) throws InvalidSftpUserException {
        try {
            if (Objects.isNull(publicKey)) {
                return;
            }
            SshKeyUtils sshKeyUtils = new SshKeyUtils(publicKey);
            if (!sshKeyUtils.isValidPublicKey()) {
                throw new InvalidSftpUserException(String.format("Invalid public key passed %s", publicKey));
            }
            this.publicKey = sshKeyUtils.sshPublicKey;
        } catch (InvalidKeyException e) {
            e.printStackTrace();
            throw new InvalidSftpUserException(e.getMessage());
        }
    }

    public void setPrivateKey(String privateKey) throws InvalidSftpUserException {
        try {
            if (Objects.isNull(privateKey)) {
                return;
            }
            if (privateKey.startsWith("generate")) {
                String[] generateInfo = privateKey.split(" ");
                if (generateInfo.length != 3) {
                    throw new InvalidSftpUserException(
                            String.format(
                                    "Must use syntax 'generate <ssh key algorithm>' for generating a new key. passed '%s'",
                                    privateKey
                            )
                    );
                }
                SshKeyUtils sshKeyUtils = new SshKeyUtils();
                try {
                    sshKeyUtils.generateSshKeyPair(generateInfo[1], Integer.valueOf(generateInfo[2]));
                    this.publicKey = sshKeyUtils.sshPublicKey;
                    this.privateKeyRaw = sshKeyUtils.privateKey;
                    return;
                } catch (GeneralSecurityException | IOException e) {
                    e.printStackTrace();
                    throw new InvalidSftpUserException(e.getMessage());
                }
            }
            SshKeyUtils sshKeyUtils = new SshKeyUtils(publicKey, privateKey);
            if (!sshKeyUtils.validKeyPair()) {
                throw new InvalidSftpUserException(String.format("Invalid private key for public key %s", publicKey));
            }
            this.privateKeyRaw = sshKeyUtils.privateKey;
        } catch (InvalidKeyException e) {
            e.printStackTrace();
            throw new InvalidSftpUserException(e.getMessage());
        }
    }

    public void setSftpServerId(String sftpServerId) {
        this.sftpServerId = sftpServerId;
    }

    public void setPds(List<String> pds) {
        setPds(new HashSet<>(pds));
    }

    // todo: verify PDS string format
    public void setPds(Set<String> pds) {
        if (Objects.nonNull(pds)) {
            Set<String> removePds = new HashSet<>();
            for (String item : pds) {
                if (item.startsWith(("-"))) {
                    removePds.add(item.replace("-", ""));
                }
            }
            for (String item : removePds) {
                pds.remove(item);
                pds.remove("-" + item);
            }
        }
        this.pds = pds;
    }


    public void setAcceptedIpNetwork(List<String> acceptedIpNetwork) throws InvalidSftpUserException {
        setAcceptedIpNetwork(new HashSet<>(acceptedIpNetwork));
    }

    public void setAcceptedIpNetwork(Set<String> acceptedIpNetwork) throws InvalidSftpUserException {
        if (Objects.isNull(acceptedIpNetwork) || acceptedIpNetwork.size() == 0) {
            throw new InvalidSftpUserException("Must have at least one IP whitelist");
        }
        if (acceptedIpNetwork.contains(null)) {
            throw new InvalidSftpUserException("Network addresses must not be null");
        }


        Pair<Set<String>, Set<String>> networksTuple = validateNetworks(acceptedIpNetwork);
        this.removedCidrs = networksTuple.getRight();
        this.acceptedIpNetwork = networksTuple.getLeft();
        validateAcceptedIpcidrs();
    }

    public void removeAcceptedIpNetwork(Set<String> removeCidrs) throws InvalidSftpUserException {
        removeCidrs.addAll(this.removedCidrs);
        Set<String> translatedRemoveCidrs = removeCidrs.stream()
                .map(cidr -> String.format("-%s", cidr))
                .collect(Collectors.toSet());
        translatedRemoveCidrs.addAll(this.acceptedIpNetwork);
        Pair<Set<String>, Set<String>> networksTuple = validateNetworks(translatedRemoveCidrs);
        this.acceptedIpNetwork = networksTuple.getLeft();
        this.removedCidrs.addAll(networksTuple.getRight());
        validateAcceptedIpcidrs();
    }

    public void addAcceptedIpNetwork(Set<String> addCidrs) throws InvalidSftpUserException {
        addCidrs.addAll(this.acceptedIpNetwork);
        Pair<Set<String>, Set<String>> networksTuple = validateNetworks(addCidrs);
        for (String addedCidr : networksTuple.getLeft()) {
            // this cidr was re-added so it should no longer be in the remove list
            if (this.removedCidrs.contains(addedCidr)) {
                this.removedCidrs.remove(addedCidr);
            }
        }
        this.acceptedIpNetwork = networksTuple.getLeft();
        this.removedCidrs.addAll(networksTuple.getRight());
        validateAcceptedIpcidrs();
    }

    public void validateAcceptedIpcidrs() throws InvalidSftpUserException {
        if (this.acceptedIpNetwork.isEmpty()) {
            throw new InvalidSftpUserException("Cannot remove last network");
        }
    }

    public Pair<Set<String>, Set<String>> validateNetworks(Set<String> acceptedIpNetwork) throws InvalidSftpUserException {
        Set<String> removeRanges = new HashSet<>();

        Set<String> validatedAddresses = new HashSet<>();
        SubnetUtils subnet;
        for (String networkAddress : acceptedIpNetwork) {
            Boolean removeNetwork = false;
            // assume this is a host address if no cidr is specified
            if (!networkAddress.contains("/")) {
                networkAddress = networkAddress + "/32";
            }

            // determine if this range has the removal token attached
            if (networkAddress.startsWith("-")) {
                networkAddress = networkAddress.replace("-", "");
                removeNetwork = true;
            }

            try {
                subnet = new SubnetUtils(networkAddress);
            } catch (IllegalArgumentException e) {
                throw new InvalidSftpUserException(String.format("Could not parse CIDR address %s", networkAddress));
            }

            String cidrMask = networkAddress.split("/")[1];

            if (removeNetwork) {
                removeRanges.add(networkAddress);
            } else {
                validatedAddresses.add(String.format("%s/%s", subnet.getInfo().getNetworkAddress(), cidrMask));
            }
        }
        for (String network : removeRanges) {
            validatedAddresses.remove(network);
        }
        return new MutablePair<>(validatedAddresses, removeRanges);
    }

    @Override
    public String toString() {
        return "SftpUserModel{" +
                "username='" + username + '\'' +
                ", publicKey='" + publicKey + '\'' +
                ", sftpServerId='" + sftpServerId + '\'' +
                ", pds=" + pds +
                ", acceptedIpNetwork=" + acceptedIpNetwork +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        SftpUserModel that = (SftpUserModel) o;

        return new EqualsBuilder().append(username, that.username).append(password, that.password).append(publicKey, that.publicKey).append(sftpServerId, that.sftpServerId).append(pds, that.pds).append(acceptedIpNetwork, that.acceptedIpNetwork).append(group, that.group).append(homeDirectoryDetails, that.homeDirectoryDetails).append(homeDirectoryType, that.homeDirectoryType).append(awsRole, that.awsRole).append(active, that.active).append(initialized, that.initialized).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(username).append(password).append(publicKey).append(sftpServerId).append(pds).append(acceptedIpNetwork).append(group).append(homeDirectoryDetails).append(homeDirectoryType).append(awsRole).append(active).append(initialized).toHashCode();
    }

    @JsonPOJOBuilder()
    @JsonIgnoreProperties(value = {"GROUP_WHITELIST"}, ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    public static final class SftpUserModelBuilder {
        @JsonProperty("Username")
        public String username;
        @JsonProperty("Password")
        public String password;
        @JsonProperty("PublicKey")
        public String publicKey;
        @JsonProperty("PrivateKey")
        public String privateKey;
        @JsonProperty("SFTPServerID")
        public String sftpServerId;
        @JsonProperty("PDS")
        public Set<String> pds;
        @JsonProperty("AcceptedIpNetwork")
        @JsonFormat(with = Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        public Set<String> acceptedIpNetwork;
        @JsonProperty("Group")
        public String group;
        @JsonProperty("HomeDirectoryDetails")
        public String homeDirectoryDetails;
        @JsonProperty("HomeDirectoryType")
        public String homeDirectoryType;
        @JsonProperty("Role")
        public String awsRole;
        @JsonProperty("Active")
        public Boolean active = true;
        @JsonProperty("Initialized")
        public Boolean initialized = false;


        private SftpUserModelBuilder() {
        }

        public static SftpUserModelBuilder builder() {
            return new SftpUserModelBuilder();
        }

        public SftpUserModelBuilder withUsername(String username) {
            this.username = username;
            return this;
        }

        public SftpUserModelBuilder withPassword(String password) {
            this.password = password;
            return this;
        }

        public SftpUserModelBuilder withPublicKey(String publicKey) {
            this.publicKey = publicKey;
            return this;
        }

        public SftpUserModelBuilder withPrivateKey(String privateKey) {
            this.privateKey = privateKey;
            return this;
        }

        public SftpUserModelBuilder withSftpServerId(String sftpServerId) {
            this.sftpServerId = sftpServerId;
            return this;
        }

        public SftpUserModelBuilder withPds(List<String> pds) {
            this.pds = new HashSet<>(pds);
            return this;
        }

        public SftpUserModelBuilder withAcceptedIpNetwork(List<String> acceptedIpNetwork) {
            if (acceptedIpNetwork.size() == 1) {
                acceptedIpNetwork = Arrays.asList(acceptedIpNetwork.get(0).split(","));
            }
            this.acceptedIpNetwork = new HashSet<>(acceptedIpNetwork);
            return this;
        }

        public SftpUserModelBuilder withGroup(String group) {
            this.group = group;
            return this;
        }

        public SftpUserModelBuilder withHomeDirectoryDetails(String homeDirectoryDetails) {
            this.homeDirectoryDetails = homeDirectoryDetails;
            return this;
        }

        public SftpUserModelBuilder withHomeDirectoryType(String homeDirectoryType) {
            this.homeDirectoryType = homeDirectoryType;
            return this;
        }

        public SftpUserModelBuilder withAwsRole(String awsRole) {
            this.awsRole = awsRole;
            return this;
        }

        public SftpUserModelBuilder withActive(Boolean active) {
            this.active = active;
            return this;
        }

        public SftpUserModelBuilder withInitialized(Boolean initialized) {
            this.initialized = initialized;
            return this;
        }

        public SftpUserModel build() throws InvalidSftpUserException {
            SftpUserModel sftpUserModel = new SftpUserModel();
            sftpUserModel.setUsername(username);
            sftpUserModel.setPassword(password);
            sftpUserModel.setPublicKey(publicKey);
            sftpUserModel.setPrivateKey(privateKey);
            sftpUserModel.setSftpServerId(sftpServerId);
            sftpUserModel.setPds(pds);
            sftpUserModel.setAcceptedIpNetwork(acceptedIpNetwork);
            sftpUserModel.setGroup(group);
            sftpUserModel.setHomeDirectoryDetails(homeDirectoryDetails);
            sftpUserModel.setHomeDirectoryType(homeDirectoryType);
            sftpUserModel.setAwsRole(awsRole);
            sftpUserModel.setActive(active);
            sftpUserModel.setInitialized(initialized);
            return sftpUserModel;
        }
    }
}
