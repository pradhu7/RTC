package com.apixio.sdk.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import com.apixio.datasource.s3.S3Ops;
import com.apixio.sdk.FxLogger;
import com.apixio.sdk.logging.DefaultLogger;
import com.apixio.util.S3BucketKey;
import com.apixio.util.UriFetcher;

/**
 * A URI fetcher that handles s3:// and file:/// and http:// and https:// URIs.  For HTTP/S URIs
 * it also has the ability to conditionally add HTTP Basic Auth header.
 */
public class AllUriFetcher implements UriFetcher
{

    /**
     * BasicAuth is triggered by a match in the URI prefix with the serverPrefix as recorded here.
     */
    public static class BasicAuthInfo
    {
        private String serverPrefix;
        private String username;
        private String password;

        public String getServerPrefix()
        {
            return serverPrefix;
        }
        public String getUsername()
        {
            return username;
        }
        public String getPassword()
        {
            return password;
        }

        public BasicAuthInfo(String serverPrefix, String username, String password)
        {
            this.serverPrefix = serverPrefix.toLowerCase();
            this.username     = username;
            this.password     = password;
        }
    }

    /**
     * Fields
     */
    private FxLogger              logger;
    private S3Ops                 s3Ops;
    private List<BasicAuthInfo>   basicAuths;
    private CloseableHttpClient   httpClient;
    private static Base64.Encoder b64Encoder = Base64.getEncoder();

    public AllUriFetcher(S3Ops s3Ops, List<BasicAuthInfo> basicAuths)
    {
        this(s3Ops, basicAuths, new DefaultLogger());
    }

    public AllUriFetcher(S3Ops s3Ops, List<BasicAuthInfo> basicAuths, FxLogger logger)
    {
        this.logger     = logger;
        this.s3Ops      = s3Ops;
        this.basicAuths = basicAuths;

        createHttpClient(10000, 10000, 10000); //!! hardcode 10sec for all timeouts for now
    }

    /**
     * Create a simple Apache HttpClient object with the given timeouts
     */
    private void createHttpClient(int socketTimeoutMs, int connectTimeoutMs, int connectionRequestTimeoutMs)
    {
        RequestConfig requestConfig = RequestConfig.custom()
                .setSocketTimeout(socketTimeoutMs)
                .setConnectTimeout(connectTimeoutMs)
                .setConnectionRequestTimeout(connectionRequestTimeoutMs)
                .build();

        httpClient = HttpClientBuilder.create()
                .setDefaultRequestConfig(requestConfig)
                .setMaxConnPerRoute(100)
                .setMaxConnTotal(200)
                .setConnectionTimeToLive(1, TimeUnit.MINUTES)
                .build();
    }

    /**
     * General fetcher of URI content that just dispatches to scheme-specific method
     */
    public InputStream fetchUri(URI uri) throws IOException
    {
        String scheme = uri.getScheme();

        if (scheme == null)
            throw new IllegalArgumentException("Attempt to fetch a URI without a scheme:  " + uri);

        logger.debug("Fetching URI %s", uri);

        switch (scheme.toLowerCase())
        {
            case "s3":
                return fetchS3Object(uri);

            case "file":
                return fetchFile(uri);

            case "http":
            case "https":
                return fetchHttpObject(uri);

            default:
                throw new IllegalArgumentException("Unsupported URI scheme " + scheme + " in fetching URIs");
        }
    }

    private InputStream fetchS3Object(URI uri) throws IOException
    {
        S3BucketKey bk = new S3BucketKey(uri);

        return s3Ops.getObject(bk.getBucket(), bk.getKey());
    }

    private InputStream fetchFile(URI uri) throws IOException
    {
        return new FileInputStream(new File(uri.getPath()));
    }

    /**
     * Fetches an object that has an HTTP/HTTPS URI.  It does this by first pulling the object down
     * to a temporary file and then it returns an InputStream to that file.  This is done as there
     * doesn't seem to be a way to cleanly close the HttpResponse from just the resturned InputStream.
     * (If there is such a way, then this code should change.)
     */
    private InputStream fetchHttpObject(URI uri) throws IOException
    {
        RequestBuilder        builder  = RequestBuilder.create("GET").setCharset(StandardCharsets.UTF_8).setUri(uri);
        CloseableHttpResponse response = null;

        addBasicAuth(uri, builder);

        try
        {
            response = httpClient.execute(builder.build());

            if (response.getStatusLine().getStatusCode() == 200)
            {
                File local = File.createTempFile("urif-", ".tmp");

                local.deleteOnExit();

                try (OutputStream fos = new FileOutputStream(local))
                {
                    response.getEntity().writeTo(fos);
                }

                return new FileInputStream(local);
            }
            else
            {
                throw new IOException("Received code " + response.getStatusLine().getStatusCode() + " for GET on " + uri);
            }
        }
        finally
        {
            if (response != null)
                HttpClientUtils.closeQuietly(response);
        }
    }

    /**
     * Check if the URI matches a basic auth setting and add the Authorization header if it does
     */
    private void addBasicAuth(URI uri, RequestBuilder builder)
    {
        if (basicAuths.size() > 0)
        {
            String uriStr = uri.toString().toLowerCase();

            for (BasicAuthInfo basicAuth : basicAuths)
            {
                if (uriStr.startsWith(basicAuth.serverPrefix))
                {
                    // skip all the complexity of HttpClient's auth provider stuff:
                    builder.addHeader("Authorization",
                                      ("Basic " +
                                       b64Encoder.encodeToString((basicAuth.username +
                                                                  ":" +
                                                                  basicAuth.password).getBytes(StandardCharsets.UTF_8)))
                        );

                    break;
                }
            }
        }
        else
        {
            logger.info("No addBasicAuths added to AllUriFetcher");
        }
    }
            
}

