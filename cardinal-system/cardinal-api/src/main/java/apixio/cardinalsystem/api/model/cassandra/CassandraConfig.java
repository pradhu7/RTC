package apixio.cardinalsystem.api.model.cassandra;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Feature;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

public class CassandraConfig implements Serializable {
    @JsonFormat(with = Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<String> host;
    private String keyspace = "cardinal";
    private String datacenter = "aws-us-west-2";
    private int port = 9042;
    private String username;
    @JsonProperty(access = Access.WRITE_ONLY)
    private String password;
    private Long baseDelayMs = 500L;
    private Long maxDelayMs = 5000L;
    private int minConnections = 2;
    private int maxConnections = 50;
    private int readAttempts = 0;
    private int writeAttempts = 0;
    private int unavailableAttemps = 0;
    private int tcpReadTimeout = 5000;
    private int tcpConnectTimeout = 10000;
    private String flinkConcurrentRequestTimeout = "10min";
    private int flinkMaxConcurrentRequests = 10000;

    public String getKeyspace() {
        return keyspace;
    }

    public void setKeyspace(String keyspace) {
        this.keyspace = keyspace;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public List<String> getHost() {
        return host;
    }

    public void setHost(List<String> host) {
        if (host.size() == 1) {
            host = Arrays.asList(host.get(0).split(","));
        }
        this.host = host;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDatacenter() {
        return datacenter;
    }

    public void setDatacenter(String datacenter) {
        this.datacenter = datacenter;
    }

    public Long getBaseDelayMs() {
        return baseDelayMs;
    }

    public void setBaseDelayMs(Long baseDelayMs) {
        this.baseDelayMs = baseDelayMs;
    }

    public Long getMaxDelayMs() {
        return maxDelayMs;
    }

    public void setMaxDelayMs(Long maxDelayMs) {
        this.maxDelayMs = maxDelayMs;
    }

    public int getMinConnections() {
        return minConnections;
    }

    public void setMinConnections(int minConnections) {
        this.minConnections = minConnections;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }

    public int getReadAttempts() {
        return readAttempts;
    }

    public void setReadAttempts(int readAttempts) {
        this.readAttempts = readAttempts;
    }

    public int getWriteAttempts() {
        return writeAttempts;
    }

    public void setWriteAttempts(int writeAttempts) {
        this.writeAttempts = writeAttempts;
    }

    public int getUnavailableAttemps() {
        return unavailableAttemps;
    }

    public void setUnavailableAttemps(int unavailableAttemps) {
        this.unavailableAttemps = unavailableAttemps;
    }

    public int getTcpReadTimeout() {
        return tcpReadTimeout;
    }

    public void setTcpReadTimeout(int tcpReadTimeout) {
        this.tcpReadTimeout = tcpReadTimeout;
    }

    public int getTcpConnectTimeout() {
        return tcpConnectTimeout;
    }

    public void setTcpConnectTimeout(int tcpConnectTimeout) {
        this.tcpConnectTimeout = tcpConnectTimeout;
    }

    public String getFlinkConcurrentRequestTimeout() {
        return flinkConcurrentRequestTimeout;
    }

    public void setFlinkConcurrentRequestTimeout(String flinkConcurrentRequestTimeout) {
        this.flinkConcurrentRequestTimeout = flinkConcurrentRequestTimeout;
    }

    public int getFlinkMaxConcurrentRequests() {
        return flinkMaxConcurrentRequests;
    }

    public void setFlinkMaxConcurrentRequests(int flinkMaxConcurrentRequests) {
        this.flinkMaxConcurrentRequests = flinkMaxConcurrentRequests;
    }
}
