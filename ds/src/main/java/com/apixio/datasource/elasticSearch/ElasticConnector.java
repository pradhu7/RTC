package com.apixio.datasource.elasticSearch;

import com.apixio.security.Security;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.util.Arrays;



public class ElasticConnector {

    private final static Logger logger = LoggerFactory.getLogger(ElasticConnector.class);

    private RestClientBuilder lowLevelClientBulider;
    private RestHighLevelClient highLevelClient;

    private String[]            hosts;
    private int                 binaryPort;

    private int threadCount = 1;
    private int connectionTimeoutMs = -1;
    private int socketTimeoutMs     = -1;

    private Security security;

    public ElasticConnector()
    {
        security = Security.getInstance();
    }

    public void close()
    {   try {
            highLevelClient.close();
        } catch (Exception e) {
            logger.error("Could not shut down elastic search connector ", e);
            System.out.println("Could not shut down elastic search connector " + e);
        }
    }

    public void setHosts(String hosts)
    {
        this.hosts = hosts.split(",");
    }

    public void setBinaryPort(int binaryPort)
    {
        this.binaryPort = binaryPort;
    }

    public void setThreadCount(int threadCount)
    {
        this.threadCount = threadCount;
    }

    public void setSecurity(Security sec)
    {
        this.security = sec;
    }

    public int getConnectionTimeoutMs()
    {
        return connectionTimeoutMs;
    }

    public void setConnectionTimeoutMs(int connectionTimeoutMs)
    {
        if (connectionTimeoutMs >= 0)
        {
            this.connectionTimeoutMs = connectionTimeoutMs;
        }
    }

    public int getSocketTimeoutMs()
    {
        return socketTimeoutMs;
    }

    public void setSocketTimeoutMs(int socketTimeoutMs)
    {
        if (connectionTimeoutMs >= 0)
        {
            this.socketTimeoutMs = socketTimeoutMs;
        }
    }

    public RestClientBuilder getLowLevelClientBuilder()
    {
        return lowLevelClientBulider;
    }

    public RestHighLevelClient getHighLevelClient() { return highLevelClient; }

    /**
     * init() sets up the actual elastic search connection.
     */
    public void init() throws Exception
    {
        HttpHost[] httpHosts = Arrays.stream(hosts)
                .map(host -> new HttpHost(host, binaryPort, "https"))
                .toArray(HttpHost[]::new);
        if (connectionTimeoutMs >= 0 && socketTimeoutMs >= 0 && threadCount >= 1)
        {
            RestClientBuilder restClientBuilder = RestClient.builder(httpHosts)
                    .setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
                    @Override
                    public HttpAsyncClientBuilder customizeHttpClient(
                            HttpAsyncClientBuilder httpClientBuilder) {
                        return httpClientBuilder.setDefaultIOReactorConfig(
                                IOReactorConfig.custom()
                                        .setIoThreadCount(threadCount)
                                        .build());
                              }
                    })
                    .setRequestConfigCallback(
                        new RestClientBuilder.RequestConfigCallback() {
                            @Override
                            public RequestConfig.Builder customizeRequestConfig(
                                    RequestConfig.Builder requestConfigBuilder) {
                                return requestConfigBuilder
                                        .setConnectTimeout(connectionTimeoutMs)
                                        .setSocketTimeout(socketTimeoutMs);
                            }
                        });

            // TODO delete SSL hack after we have a real cluster
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{ UnsafeX509ExtendedTrustManager.getInstance() }, null);

            restClientBuilder.setHttpClientConfigCallback(httpClientBuilder ->
                    httpClientBuilder.setSSLContext(sslContext)
                            .setSSLHostnameVerifier((host, session) -> true));

            lowLevelClientBulider = restClientBuilder;
            highLevelClient = new RestHighLevelClient(lowLevelClientBulider);
        }
        else
        {
            // TODO delete SSL hack after we have a real cluster
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{ UnsafeX509ExtendedTrustManager.getInstance() }, null);

            // instantiate with elastic defaults
            RestClientBuilder restClientBuilder = RestClient.builder(httpHosts)
                    .setHttpClientConfigCallback(httpClientBuilder ->
                            httpClientBuilder.setSSLContext(sslContext)
                                    .setSSLHostnameVerifier((host, session) -> true));
            lowLevelClientBulider = restClientBuilder;
            highLevelClient = new RestHighLevelClient(lowLevelClientBulider);
        }
    }
}