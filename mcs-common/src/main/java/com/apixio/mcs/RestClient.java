package com.apixio.mcs;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.util.JsonFormat;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import com.apixio.mcs.client.util.ConfigUtil;
import com.apixio.XUUID;
import com.apixio.fxs.FunctionDefinition;
import com.apixio.ifcwrapper.util.JsonUtil;     //NO!! this seems to be not really necessary as nothing (de)serialized here uses XUUIDs
import com.apixio.mcs.meta.McMeta.FullMeta;     // from .proto
import com.apixio.sdk.protos.FxProtos.FxDef;    // from .proto

/**
 * This is a utility class usable by a client of MC Server.  This class was originally in
 * apx-modelcatalogclient/common but has been moved here to be a generic interface into
 * MCS (the original one had some (old) domain-specific constructs such as config.yaml being
 * required on a ModelCombination--that has been removed).
 */
public class RestClient
{

    private static final Logger LOG = LoggerFactory.getLogger(RestClient.class);

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final static int RETRY_MAX = 10;

    private final URI baseMcsServiceUri;
    private final CloseableHttpClient httpClient;

    /**
     * A way to regularize HTTP request code and to minimize repeated code/patterns.  The use model is
     * that a single instance of this class (as subclassed within each method body) can be used only
     * once.
     */
    static abstract class Template<R>
    {
        protected CloseableHttpResponse response = null;

        /**
         * Outer method code calls this to have standard/correct closing of HTTP request.
         * Note that the requestUri param is solely for including it in the exception info.
         */
        R execute(URI requestUri) throws McsServiceException
        {
            long startMs = System.currentTimeMillis();

            System.out.println("[DEBUG] MCS RestClient making request " + requestUri);

            try
            {
                return logic(requestUri);
            }
            catch (Exception e)
            {
                throw new McsServiceException("Failure in request " + requestUri, e);
            }
            finally
            {
                //                System.out.println("[DEBUG] request to " + requestUri + " took " + (System.currentTimeMillis() - startMs) + " ms");

                if (response != null)
                    HttpClientUtils.closeQuietly(response);
            }
        }

        /**
         * Code to be wrapped with standard wrapper code in execute() goes here
         */
        abstract R logic(URI requestUri) throws Exception;
    }

    /**
     *
     */
    public RestClient(URI baseMcsServiceUri)
    {
        this(baseMcsServiceUri, 5000, 5000, 5000);
    }

    public RestClient(URI baseMcsServiceUri, int socketTimeout, int connectTimeout, int connectionRequestTimeout)
    {
        RequestConfig requestConfig = RequestConfig.custom()
                .setSocketTimeout(socketTimeout)
                .setConnectTimeout(connectTimeout)
                .setConnectionRequestTimeout(connectionRequestTimeout)
                .build();
        this.baseMcsServiceUri = baseMcsServiceUri;
        this.httpClient = HttpClientBuilder.create()
                .setDefaultRequestConfig(requestConfig)
                .setMaxConnPerRoute(100)
                .setMaxConnTotal(200)
                .setConnectionTimeToLive(1, TimeUnit.MINUTES)
                .build();
        LOG.info("RestClient created with endpoint:" + baseMcsServiceUri.toString());
    }

    /**
     * @return Optional.empty() if this logical id doesn't match any mcId, or the mcId otherwise.
     */
    public Optional<String> getMcId(String logicalId) throws McsServiceException
    {
        URI                        requestUri   = baseMcsServiceUri.resolve("/mcs/logids/" + safeUrlEncode(logicalId) + "/owner");
        Template<Optional<String>> exec;

        exec = new Template<Optional<String>>() {
                @Override
                Optional<String> logic(URI requestUri) throws Exception
                {
                    HttpUriRequest request = makeStandardRequest("GET", requestUri).build();
                    String         body;
                    ModelMeta      meta;

                    response = getAndRetry(request, RETRY_MAX);
                    body     = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);

                    if (body == null || body.trim().isEmpty())
                        return Optional.empty();

                    meta = JsonUtil.getMapper().readerFor(ModelMeta.class).readValue(body);

                    return Optional.ofNullable(meta.id);
                }
            };

        return exec.execute(requestUri);
    }

    /**
     * Retrieves the metadata for the given MCID.
     */
    public ModelMeta getModelCombinationMeta(XUUID mcid) throws McsServiceException
    {
        URI                 requestUri = baseMcsServiceUri.resolve("/mcs/models/" + mcid + "/metadata");
        Template<ModelMeta> exec;

        exec = new Template<ModelMeta>() {
                @Override
                ModelMeta logic(URI requestUri) throws Exception
                {
                    HttpUriRequest request = makeStandardRequest("GET", requestUri).build();
                    String         body;

                    response = getAndRetry(request, RETRY_MAX);
                    body     = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);

                    if ((body == null) || body.trim().isEmpty())
                        return null;
                    else
                        return JsonUtil.getMapper().readerFor(ModelMeta.class).readValue(body);
                }
            };

        return exec.execute(requestUri);
    }

    /**
     * Gets the "full" meta for the given MCID.  Full meta just adds the list of metadata for
     * each of the parts to the parent metadata.
     */
    public FullMeta getServerMCMeta(String mcId) throws McsServiceException
    {
        URI                requestUri = baseMcsServiceUri.resolve("/mcs/models/" + mcId + "/metadata");
        Template<FullMeta> exec;

        exec = new Template<FullMeta>() {
                @Override
                FullMeta logic(URI requestUri) throws Exception
                {
                    HttpUriRequest   request = makeStandardRequest("GET", requestUri).build();
                    String           body;
                    FullMeta.Builder builder;

                    response = getAndRetry(request, RETRY_MAX);
                    body     = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);

                    builder = FullMeta.newBuilder();
                    JsonFormat.parser().ignoringUnknownFields().merge(body, builder);

                    return builder.build();
                }
            };

        return exec.execute(requestUri);
    }

    public List<FullMeta> getAllMcIds() throws McsServiceException {
        URI                      requestUri = baseMcsServiceUri.resolve("/mcs/models");
        Template<List<FullMeta>> exec;

        exec = new Template<List<FullMeta>>() {
                @Override
                List<FullMeta> logic(URI requestUri) throws Exception
                {
                    HttpUriRequest request = makeStandardRequest("GET", requestUri).build();
                    List<FullMeta> fullMetaList;
                    String         body;

                    response = getAndRetry(request, RETRY_MAX);
                    body     = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);

                    if (body == null || body.trim().isEmpty())
                        return Collections.emptyList();

                    fullMetaList = new ArrayList<>();

                    for (Object element : JsonUtil.getMapper().readValue(body, List.class))
                    {
                            FullMeta.Builder builder = FullMeta.newBuilder();
                            String           json = JsonUtil.getMapper().writeValueAsString(element);

                            JsonFormat.parser().ignoringUnknownFields().merge(json, builder);
                            fullMetaList.add(builder.build());
                    }
                    return fullMetaList;
                }
            };

        return exec.execute(requestUri);
    }

    /****************************************************************************
     *
     * Dependency Retrieval Methods
     *
     */

    public List<LogicalMeta> getDepsByLogicalId(String logicalId) throws McsServiceException
    {
        URI                         requestUri = baseMcsServiceUri.resolve("/mcs/logids/" + safeUrlEncode(logicalId) + "/deps/owners");
        Template<List<LogicalMeta>> exec;

        exec = new Template<List<LogicalMeta>>() {
                @Override
                List<LogicalMeta> logic(URI requestUri) throws Exception
                {
                    HttpUriRequest request = makeStandardRequest("GET", requestUri).build();
                    String         body;

                    response = getAndRetry(request, RETRY_MAX);
                    body = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);

                    if (body == null || body.trim().isEmpty())
                        return Collections.emptyList();

                    return JsonUtil.getMapper().readerFor(new TypeReference<List<LogicalMeta>>() { }).readValue(body);
                }
            };

        return exec.execute(requestUri);
    }


    public List<LogicalMeta> getDepsByMcId(String mcId) throws McsServiceException
    {
        URI                         requestUri = baseMcsServiceUri.resolve("/mcs/models/" + mcId + "/deps/owners");
        Template<List<LogicalMeta>> exec;

        exec = new Template<List<LogicalMeta>>() {
                @Override
                List<LogicalMeta> logic(URI requestUri) throws Exception
                {
                    HttpUriRequest request = makeStandardRequest("GET", requestUri).build();
                    String         body;

                    response = getAndRetry(request, RETRY_MAX);
                    body     = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);

                    if (body == null || body.trim().isEmpty())
                        return Collections.emptyList();
                    
                    return JsonUtil.getMapper().readerFor(new TypeReference<List<LogicalMeta>>() { }).readValue(body);
                }
            };

        return exec.execute(requestUri);
    }

    /****************************************************************************
     *
     * Model Creation and Update methods
     *
     */

    /**
     * Add a part to an existing model combination.  Creator is usually the email address of the user
     * adding the part.
     */
    public void createPart(String mcId, String partName, String partMimeType, InputStream partData, String creator) throws McsServiceException
    {
        createPartCommon(mcId, partName, partMimeType,
                         baseMcsServiceUri.resolve("/mcs/models/" + mcId + "/parts/" + partName),
                         creator,
                         partData);
    }

    public void createPartByRef(String mcId, String partName, String partMimeType, String md5Hash, String creator) throws McsServiceException
    {
        createPartCommon(mcId, partName, partMimeType,
                         baseMcsServiceUri.resolve("/mcs/models/" + mcId + "/parts/" + partName + "?ref=" + md5Hash),
                         creator,
                         new ByteArrayInputStream(new byte[0]));
    }

    /**
     * Common implementation for creating a part
     */
    private void createPartCommon(String mcId, String partName, String partMimeType, URI requestUri, String creator, InputStream data) throws McsServiceException
    {
        if ((partMimeType == null) || (partMimeType.length() == 0))
            throw new IllegalArgumentException("MC Part's MIME type is required");

        Template<Void> exec;

        exec = new Template<Void>() {
                @Override
                Void logic(URI requestUri) throws Exception
                {
                    RequestBuilder builder = makeStandardRequest("PUT", requestUri);
                    HttpUriRequest request;
                    int            statusCode;

                    builder.setEntity(new InputStreamEntity(data)).addHeader("Content-Type", partMimeType).addHeader("Apixio-Uploaded-By", creator);
                    request = builder.build();

                    response   = httpClient.execute(request);
                    statusCode = response.getStatusLine().getStatusCode();

                    if (statusCode != 200)
                    {
                        LOG.error("NOT OK: " + statusCode);
                        LOG.error(IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8));

                        throw new McsServiceException(statusCode, "PUT", requestUri);
                    }

                    return null;  // blech -- for Void
                }
            };

        exec.execute(requestUri);
    }


    /**
     * Find a part by its MD5 hash
     */
    public PartMeta findPart(String md5Hash) throws McsServiceException
    {
        URI                requestUri = baseMcsServiceUri.resolve("/mcs/parts?md5=" + md5Hash);
        Template<PartMeta> exec;

        exec = new Template<PartMeta>() {
                @Override
                PartMeta logic(URI requestUri) throws Exception
                {
                    HttpUriRequest request = makeStandardRequest("GET", requestUri).build();
                    String         json;
                    List<PartMeta> metas;

                    // since MCS parts share by MD5 hash, the returned JSON is an array with
                    // 0 or 1 elements

                    response = getAndRetry(request, RETRY_MAX);
                    json     = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);

                    // no need to use JsonUtil.getMapper as we're not deserialing XUUIDs
                    metas = objectMapper.readerFor(new TypeReference<List<PartMeta>>() { }).readValue(json);

                    return (metas.size() > 0) ? metas.get(0) : null;
                }
            };

        return exec.execute(requestUri);
    }

    /**
     * Create the top-level (parent) model combination using the given metadata.
     */
    public XUUID createModelCombination(ModelMeta meta) throws McsServiceException
    {
        return createEntity(baseMcsServiceUri.resolve("/mcs/models"), meta);
    }

    /**
     * Set the owner of the given logicalID to the given MCID.  This allows the call to
     *
     *  getMcId(String logicalID)
     *
     * to succeed.
     */
    public void claimOwnershipOfLogicalIDs(String mcId, List<String> logicalIDs) throws McsServiceException
    {
        URI            requestUri = baseMcsServiceUri.resolve("/mcs/models/" + mcId + "/owns");
        Template<Void> exec;

        exec = new Template<Void>() {
                @Override
                Void logic(URI requestUri) throws Exception
                {
                    StringEntity   entity  = new StringEntity(makeJsonList(logicalIDs));
                    RequestBuilder builder = makeStandardRequest("PUT", requestUri);
                    HttpUriRequest request;
                    int            statusCode;

                    builder.setEntity(entity).addHeader("Content-Type", "application/json");
                    request = builder.build();

                    response   = httpClient.execute(request);
                    statusCode = response.getStatusLine().getStatusCode();

                    if (statusCode != 200)
                    {
                        LOG.error("NOT OK: " + statusCode);
                        LOG.error(IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8));

                        throw new McsServiceException(statusCode, "PUT", requestUri);
                    }

                    return null;
                }
            };

        exec.execute(requestUri);
    }

    /**
     * Sets the dependencies for the given left logicalID.  The "left" logicalID will have a "uses"
     * relationship to this list.  Additionally this "left" logicalID will be the one claimed by
     * a given MCID.
     */
    public void setLogicalDependencies(String leftLogicalID, List<String> logicalIDs) throws McsServiceException
    {
        URI            requestUri = baseMcsServiceUri.resolve("/mcs/logids/" + safeUrlEncode(leftLogicalID) + "/deps");
        Template<Void> exec;

        exec = new Template<Void>() {
                @Override
                Void logic(URI requestUri) throws Exception
                {
                    StringEntity   entity  = new StringEntity(makeJsonList(logicalIDs));
                    RequestBuilder builder = makeStandardRequest("PUT", requestUri);
                    HttpUriRequest request;
                    int            statusCode;

                    builder.setEntity(entity).addHeader("Content-Type", "application/json");
                    request = builder.build();

                    response   = httpClient.execute(request);
                    statusCode = response.getStatusLine().getStatusCode();

                    if (statusCode != 200)
                    {
                        LOG.error("NOT OK: " + statusCode);
                        LOG.error(IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8));

                        throw new McsServiceException(statusCode, "PUT", requestUri);
                    }

                    return null;
                }
            };

        exec.execute(requestUri);
    }

    /**
     * Create/find a new FxDef from the given info.  If the parsed IDL matches an existing FxDef then that FXID
     * is returned rather than a new FXID being created.
     */
    public XUUID createFxDef(String idl, String name, String creator, String description) throws McsServiceException
    {
        return createEntity(baseMcsServiceUri.resolve("/fx/fxdefs"), new FunctionDefinition(idl, name, description, creator));
    }

    /**
     * Fetch and restore the serialized protobuf FxDef for the given FXID
     */
    public FxDef getFxDef(XUUID fxid) throws McsServiceException
    {
        URI             requestUri = baseMcsServiceUri.resolve("/fx/fxdefs/" + fxid);
        Template<FxDef> exec;

        exec = new Template<FxDef>() {
                @Override
                FxDef logic(URI requestUri) throws Exception
                {
                    HttpUriRequest request = makeStandardRequest("GET", requestUri).build();
                    String         body;
                    FxDef.Builder  builder;

                    response = getAndRetry(request, RETRY_MAX);
                    body     = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);

                    builder = FxDef.newBuilder();

                    JsonFormat.parser().ignoringUnknownFields().merge(body, builder);

                    return builder.build();
                }
            };

        return exec.execute(requestUri);
    }

    /****************************************************************************
     *
     * Private methods
     *
     */

    /**
     * Reusable/generic creation of entity on server that returns an XUUID.
     */
    private XUUID createEntity(URI requestUri, Object entity) throws McsServiceException
    {
        Template<XUUID>  exec;

        exec = new Template<XUUID>() {
                @Override
                XUUID logic(URI requestUri) throws Exception
                {
                    StringEntity   strEntity = new StringEntity(objectMapper.writeValueAsString(entity), ContentType.APPLICATION_JSON);
                    RequestBuilder builder   = makeStandardRequest("POST", requestUri);
                    HttpUriRequest request;
                    int            statusCode;

                    builder.setEntity(strEntity).addHeader("Content-Type", "application/json; charset=UTF-8");
                    request = builder.build();

                    response   = httpClient.execute(request);
                    statusCode = response.getStatusLine().getStatusCode();

                    if (statusCode >= 200 && statusCode < 300)
                    {
                        Header[] locations = response.getHeaders("location");

                        if ((locations != null) && (locations.length > 0))  // no javadocs on getHeaders behavior...
                        {
                            URI createdLocation  = new URI(locations[0].getValue());
                            URI relativeLocation = requestUri.relativize(createdLocation);

                            return XUUID.fromString(relativeLocation.toString());
                        }
                    }

                    throw new McsServiceException("MCS response did not contain location header " + response.toString());
                }
            };

        return exec.execute(requestUri);
    }

    /**
     *
     */
    private String safeUrlEncode(String e)
    {
        try
        {
            return URLEncoder.encode(e, StandardCharsets.UTF_8.toString());
        }
        catch (UnsupportedEncodingException uee)
        {
            throw new IllegalStateException("Can't URLEncode " + e, uee);
        }
    }

    private RequestBuilder makeStandardRequest(String httpMethod, URI uri)
    {
        return RequestBuilder.create(httpMethod).setCharset(StandardCharsets.UTF_8).setUri(uri);
    }

    private static String makeJsonList(List<String> list)
    {
        return "[\"" + String.join("\",\"", list) + "\"]";
    }

    /**
     * Execute the request, retrying up to the given max on network (etc) and server (5xx) errors.  An
     * exception will be thrown if a non-successful response (2xx) is received.
     *
     * The returned CloseableHttpResponse must be closed by the calling code
     */
    protected CloseableHttpResponse getAndRetry(HttpUriRequest request, int retryMax) throws McsServiceException
    {
        CloseableHttpResponse response      = null;
        Exception             lastException = null;
        int                   statusCode    = 555;   // must be >= 500

        for (int i = 0; i < retryMax; i++)
        {
            try
            {
                if (i > 0)                  // sleep only when looping due to error
                    Thread.sleep(2000);

                response = httpClient.execute(request);

                // 2xx is successful so return
                // 3xx will be automatically handled by HttpClient
                // 4xx means a retry will theoretically just come back with same error (since it's a client-side error)
                // 5xx means a retry *might* succeed, so try again up to retryMax times
                if ((statusCode = response.getStatusLine().getStatusCode()) < 500)
                    break;
            }
            catch (IOException | InterruptedException e)
            {
                lastException = e;
                response      = null;
            }
        }

        if ((statusCode >= 200) && (statusCode < 300))
        {
            return response;
        }
        else
        {
            URI requestUri = request.getURI();

            // an error occurred so close the response and log error
            if (response != null)
            {
                try
                {
                    statusCode = response.getStatusLine().getStatusCode();  // doesn't throw exception
                    response.close();
                }
                catch (IOException e)
                {
                    statusCode    = 500;
                    lastException = e;
                }
            }

            LOG.error("GET " + requestUri + " NOT OK: " + statusCode);

            throw new McsServiceException(statusCode, "GET", requestUri, lastException);
        }
    }

}
