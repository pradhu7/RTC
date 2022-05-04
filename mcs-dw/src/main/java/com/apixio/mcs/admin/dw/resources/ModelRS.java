package com.apixio.mcs.admin.dw.resources;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.apixio.XUUID;
import com.apixio.bms.Blob;
import com.apixio.bms.PartSource;
import com.apixio.bms.Query.Operator;
import com.apixio.bms.Query.Term;
import com.apixio.bms.Query;
import com.apixio.bms.QueryBuilder;
import com.apixio.mcs.Lifecycle;
import com.apixio.mcs.LogicalMeta;
import com.apixio.mcs.McsMetadataDef;
import com.apixio.mcs.ModelCatalogService.DownloadInfo;
import com.apixio.mcs.ModelCatalogService.JsonScope;
import com.apixio.mcs.ModelCatalogService.Latest;
import com.apixio.mcs.ModelCatalogService.LifecycleException;
import com.apixio.mcs.ModelCatalogService.Operation;
import com.apixio.mcs.ModelCatalogService;
import com.apixio.mcs.ModelMeta;
import com.apixio.mcs.PartMeta;
import com.apixio.mcs.admin.McsSysServices;
import com.apixio.mcs.util.ConversionUtil;
import com.apixio.restbase.config.ConfigSetUtil;
import com.apixio.restbase.config.MicroserviceConfig;
import com.apixio.restbase.util.DateUtil;
import com.apixio.restbase.web.BaseRS;

/**
 * Handles REST APIs for /mcs/models URL prefix
 */
@Path("/mcs/models")
public class ModelRS extends BaseRS
{
    private static Logger LOGGER;

    /**
     * Allowed values for the query parameter "method" in PUT:/mcs/models/{id}/metadata
     */
    private final static String METHOD_REPLACE = "replace";
    private final static String METHOD_UPDATE  = "update";

    /**
     *
     */
    private final static String QP_INCLUDEARCHIVED = "archived";

    /**
     * Batch part add return codes
     */
    private final static String BPA_OK     = "ok";
    private final static String BPA_NO_MD5 = "noMatch";  // no existing blob had given MD5 hash

    /**
     * These are the prefixes to be used in the query paraemeter names that will trigger searches on
     * the two JSON fields.  For example, a JSON field "foo" in the "core" JSON blob would require a
     * query parameter name of "core.foo" for it to be used in the generalized query.
     *
     * These prefixes really should end in "." as that's the logical end of the prefix
     */
    private final static String JSON_QP_CORE   = "core.";
    private final static String JSON_QP_SEARCH = "search.";

    /**
     * fields
     */
    protected McsSysServices mcsServices;
    private ModelCatalogService catalogService;

    /**
     * Strings relevant to getting the latest from query results.
     */
    private final static String QP_LATEST  = "latest";     // query param key
    private final static String L_VERSION  = "version";    // query param value
    private final static String L_CREATION = "creation";   // query param value
    private final static String L_UPLOAD   = "upload";     // query param value

    /**
     * Tuple of operator and arg for convenience
     */
    private static class OperAndArg
    {
        Operator op;
        String   arg;

        OperAndArg(Operator op, String arg)
        {
            this.op = op;
            this.arg = arg;
        }

        @Override
        public String toString()
        {
            return "(" + op + " " + arg + ")";
        }
    }

    /**
     *
     */
    public ModelRS(MicroserviceConfig configuration, McsSysServices sysServices)
    {
        super(configuration, sysServices);

        this.mcsServices    = sysServices;
        this.catalogService = mcsServices.getModelCatalogService();

        LOGGER = Logger.getLogger(ModelRS.class);
    }

    /**
     * Create a new Science Model (just metadata)
     */
    @POST
    @Consumes("application/json")
    @Produces("application/json")
    public Response createBlob(@Context HttpServletRequest request, final ModelMeta modelMeta)
    {
        ApiLogger logger = super.createApiLogger(request, "POST:/mcs/models", "mcsserver");

        return super.restWrap(new RestWrap(logger)
            {
                public Response operation() throws Exception
                {
                    Blob blob = catalogService.createModelMeta(modelMeta);

                    return created("/mcs/models/" + blob.getUuid());
                }
            });
    }

    /**
     * Deletes a blob and all associated data
     */
    @DELETE
    @Path("/{id}")
    @Produces("application/json")
    public Response deleteBlob(@Context HttpServletRequest request,
                               @PathParam("id") final String id
        )
    {
        ApiLogger logger = super.createApiLogger(request, "DELETE:/mcs/models/{id}", "mcsserver");

        return super.restWrap(new RestWrap(logger)
            {
                public Response operation() throws Exception
                {
                    Blob blob = catalogService.getModelMeta(XUUID.fromString(id));

                    if (blob != null)
                    {
                        try
                        {
                            catalogService.deleteModel(blob);
                            return ok();
                        }
                        catch (LifecycleException lx)
                        {
                            return lifecycleProblem(lx);
                        }
                    }
                    else
                    {
                        return notFound();
                    }
                }
            });
    }

    /**
     * Updates an existing Science Model (just metadata).  The value of "method" determines what to
     * do with keys/values that exist already in the database.  If method=replace, then the
     * ModelMeta payload represents the entire set of metadata so it replaces the existing set of
     * metadata.  If method=update then keys/values in the database that aren't in the ModelMeta
     * payload are left untouched.  The default is to update (i.e., not to delete the metadata in
     * the database but not in the payload).
     */
    @PUT
    @Path("/{id}/metadata")
    @Consumes("application/json")
    @Produces("application/json")
    public Response updateBlob(@Context HttpServletRequest request,
                               @PathParam("id") final String id,
                               @QueryParam("method") final String method,
                               final ModelMeta modelMeta
        )
    {
        ApiLogger logger = super.createApiLogger(request, "PUT:/mcs/models/{id}", "mcsserver");

        return super.restWrap(new RestWrap(logger)
            {
                public Response operation() throws Exception
                {
                    Boolean replace = validateMethod(method);

                    if (replace == null)
                    {
                        return badRequest("Bad value for 'method' parameter; must be 'update' or 'replace'");
                    }
                    else
                    {
                        Blob blob = catalogService.getModelMeta(XUUID.fromString(id));

                        if (blob != null)
                        {
                            try
                            {
                                catalogService.updateModelMeta(blob, modelMeta, replace);
                                return ok();
                            }
                            catch (LifecycleException lx)
                            {
                                return lifecycleProblem(lx);
                            }
                        }
                        else
                        {
                            return notFound();
                        }
                    }
                }
            });
    }

    /**
     *
     */
    @PUT
    @Path("/{id}/metadata/{scope}")
    @Consumes("application/json")
    @Produces("application/json")
    public Response updateBlobJson(@Context HttpServletRequest request,
                                   @PathParam("id") final String id,
                                   @PathParam("scope") final String scoper,
                                   final Map<String, Object> meta
        )
    {
        final ApiLogger logger = super.createApiLogger(request, "PUT:/mcs/models/{id}/metadata/search", "mcsserver");
        final JsonScope scope  = getJsonScope(scoper);

        return super.restWrap(new RestWrap(logger)
            {
                public Response operation() throws Exception
                {
                    Blob blob = catalogService.getModelMeta(XUUID.fromString(id));

                    if (blob != null)
                    {
                        ModelMeta modelMeta = new ModelMeta();

                        if (scope == JsonScope.CORE)
                            modelMeta.core = meta;
                        else
                            modelMeta.search = meta;

                        try
                        {
                            catalogService.updateModelMeta(blob, modelMeta, false);
                            return ok();
                        }
                        catch (LifecycleException lx)
                        {
                            return lifecycleProblem(lx);
                        }
                    }
                    else
                    {
                        return notFound();
                    }
                }
            });
    }

    /**
     * Updates the lifecycle state of a blob
     */
    @PUT
    @Path("/{id}/state")
    @Consumes("text/plain")
    @Produces("application/json")
    public Response updateLifecycle(@Context HttpServletRequest request,
                                    @PathParam("id") final String id,
                                    final String newState
        )
    {
        ApiLogger logger = super.createApiLogger(request, "PUT:/mcs/models/{id}/state", "mcsserver");

        return super.restWrap(new RestWrap(logger)
            {
                public Response operation() throws Exception
                {
                    Blob blob = catalogService.getModelMeta(XUUID.fromString(id));

                    if (blob != null)
                    {
                        try
                        {
                            catalogService.updateLifecycleState(blob, newState);
                            return ok();
                        }
                        catch (LifecycleException lx)
                        {
                            return lifecycleProblem(lx);
                        }
                    }
                    else
                    {
                        return notFound();
                    }
                }
            });
    }

    /**
     * Returns the metadata
     */
    @GET
    @Path("/{id}/metadata")
    @Produces("application/json")
    public Response getBlobMeta(@Context HttpServletRequest request,
                                @PathParam("id") final String id
        )
    {
        ApiLogger logger = super.createApiLogger(request, "GET:/mcs/models/{id}", "mcsserver");

        return super.restWrap(new RestWrap(logger)
            {
                public Response operation() throws Exception
                {
                    Blob blob = catalogService.getModelMeta(XUUID.fromString(id));

                    if (blob != null)
                    {
                        return ok(ConversionUtil.blobToFullModelMeta(blob,
                                                                     makePartMeta(blob.getUuid().toString(),
                                                                                  catalogService.getModelParts(blob))));
                    }
                    else
                    {
                        return notFound();
                    }
                }
            });
    }


    @GET
    @Path("/{id}/metadata/{scope}")
    @Produces("application/json")
    public Response getBlobMetaSearch(@Context HttpServletRequest request,
                                      @PathParam("id") final String id,
                                      @PathParam("scope") final String scoper
        )
    {
        final ApiLogger logger = super.createApiLogger(request, "GET:/mcs/models/{id}/metadata/search", "mcsserver");
        final JsonScope scope  = getJsonScope(scoper);

        return super.restWrap(new RestWrap(logger)
            {
                public Response operation() throws Exception
                {
                    Blob blob = catalogService.getModelMeta(XUUID.fromString(id));

                    if (blob != null)
                        return ok(ConversionUtil.jsonToJson(((scope == JsonScope.CORE) ? blob.getExtra1() : blob.getExtra2())));
                    else
                        return notFound();
                }
            });
    }

    private List<PartMeta> makePartMeta(String parentId, List<Blob> blobs)
    {
        return blobs.stream().map(b -> ConversionUtil.blobToPartMeta(parentId, b))
            .collect(Collectors.toList());
    }

    /**
     * Create a part.  Part data can be uploaded or referenced ("ref" param) by an existing blob
     * that has the given md5 hash.
     */
    @PUT
    @Path("/{id}/parts/{part}")
    @Consumes("*/*")
    @Produces("application/json")
    public Response uploadPartData(@Context HttpServletRequest request,
                                   @Context HttpHeaders headers,
                                   @PathParam("id")   final String blobID,
                                   @PathParam("part") final String partName,
                                   @QueryParam("ref") final String md5ref,
                                   // legacy: client should use PUT:/mcs/models/{id}/parts
                                   @Context final UriInfo uriInfo,
                                   final InputStream input
        )
    {
        ApiLogger logger = super.createApiLogger(request, "PUT:/mcs/models/{id}/data", "mcsserver");

        return super.restWrap(new RestWrap(logger)
            {
                public Response operation() throws Exception
                {
                    String uploader = headers.getHeaderString("Apixio-Uploaded-By");

                    if (uploader == null)
                    {
                        return badRequest("Missing HTTP header Apixio-Uploaded-By");
                    }
                    else
                    {
                        Blob blob = catalogService.getModelMeta(XUUID.fromString(blobID));

                        if (blob != null)
                        {
                            // legacy:
                            if (md5ref != null)
                            {
                                PartSource src = new PartSource();

                                src.name   = partName;
                                src.source = PartSource.SOURCE_MD5 + md5ref;

                                if (catalogService.referencePartData(blob, uploader, src))
                                {
                                    return ok();
                                }
                                else
                                {
                                    return badRequest("No part with MD5 hash exists");
                                }
                            }
                            else
                            {
                                try
                                {
                                    catalogService.uploadPartData(blob, uploader, partName,
                                                                  headers.getHeaderString(HttpHeaders.CONTENT_TYPE),
                                                                  input);
                                    return ok();
                                }
                                catch (LifecycleException lx)
                                {
                                    return lifecycleProblem(lx);
                                }
                                catch (Exception x)
                                {
                                    LOGGER.error("Failed to upload part", x);
                                    throw x;
                                }
                            }
                        }
                        else
                        {
                            return notFound();
                        }
                    }
                }
            });
    }

    /**
     * Add multiple parts at a time by supplying an array of JSON objects each of which declares the
     * name of the part and its source of data and optionally its MIME type.  This method will
     * download the actual data from the source to create the part blob.  See PartSource.java for
     * info on syntax of source.
     *
     * The JSON return structure is an object where the field name is the name of the part and the
     * field value is a string code that indicates if the part was able to be created successfully,
     * or how it failed.  Codes:
     *
     * * "ok":  part was successfully added * "noMatch:  blob with given MD5 value was not found *
     * anything else:  exception/error message
     */
    @PUT
    @Path("/{id}/parts")
    @Consumes("application/json")
    @Produces("application/json")
    public Response uploadParts(@Context HttpServletRequest request,
                                @Context HttpHeaders        headers,
                                @PathParam("id") final String blobID,
                                @Context final UriInfo uriInfo,
                                final PartSource[] parts
        )
    {
        ApiLogger logger = super.createApiLogger(request, "PUT:/mcs/models/{id}/parts", "mcsserver");

        return super.restWrap(new RestWrap(logger)
            {
                public Response operation() throws Exception
                {
                    String uploader = headers.getHeaderString("Apixio-Uploaded-By");

                    if (!validatePartSources(parts))
                        return badRequest("One or more part sources is missing name or source");
                    else if (uploader == null)
                        return badRequest("Missing HTTP header Apixio-Uploaded-By");

                    List<PartSource> partsWithInvalidSources = findPartsWithInvalidSources(parts);
                    
                    if (!partsWithInvalidSources.isEmpty())
                    {
                        String invalidId = partsWithInvalidSources.get(0).source;
                        return notFound("Part source refers to a part that does not exist", invalidId);
                    }
                    else
                    {
                        Blob blob = catalogService.getModelMeta(XUUID.fromString(blobID));

                        if (blob != null)
                        {
                            Map<String, String> results = new HashMap<>();

                            // error logic is a bit twisted because BlobMgr deals with MD5 and
                            // copy-via-stream differently:  if no match on MD5 then a null
                            // is returned, and if not MD5 mode, then success unless an exception
                            // is thrown.

                            for (PartSource src : parts)
                            {
                                try
                                {
                                    if (catalogService.referencePartData(blob, uploader, src))
                                    {
                                        results.put(src.name, BPA_OK);
                                    }
                                    else
                                    {
                                        results.put(src.name, BPA_NO_MD5);
                                    }
                                }
                                catch (Exception x)
                                {
                                    results.put(src.name, x.getMessage());
                                }
                            }
                            return ok(results);
                        }
                        else
                        {
                            return notFound();
                        }
                    }
                }
            });
    }

    /**
     * Verifies that all required bits of info are present
     */
    private boolean validatePartSources(PartSource[] parts)
    {
        for (PartSource src : parts)
        {
            if (empty(src.name) || empty(src.source))
                return false;
        }

        return true;
    }

    private List<PartSource> findPartsWithInvalidSources(PartSource[] parts)
    {
        return Arrays.stream(parts).filter(part -> !catalogService.validatePartData(part))
                                   .collect(Collectors.toList());
    }

    /**
     *
     */
    @PUT
    @Path("/{id}/parts/{part}/metadata/{scope}")
    @Consumes("application/json")
    @Produces("application/json")
    public Response updatePartJson(@Context HttpServletRequest request,
                                   @PathParam("id") final String blobID,
                                   @PathParam("part") final String partName,
                                   @PathParam("scope") final String scoper,
                                   final Map<String, Object> json
        )
    {
        final ApiLogger logger = super.createApiLogger(request, "PUT:/mcs/models/{id}/parts/{part}/metadata/{scope}", "mcsserver");
        final JsonScope scope  = getJsonScope(scoper);

        return super.restWrap(new RestWrap(logger)
            {
                public Response operation() throws Exception
                {
                    Blob blob = catalogService.getModelMeta(XUUID.fromString(blobID));

                    if (blob != null)
                    {
                        try
                        {
                            if (scope == JsonScope.CORE)
                                catalogService.updatePartMeta(blob, partName, json, null);
                            else if (scope == JsonScope.SEARCH)
                                catalogService.updatePartMeta(blob, partName, null, json);

                            return ok();
                        }
                        catch (LifecycleException lx)
                        {
                            return lifecycleProblem(lx);
                        }
                    }
                    else
                    {
                        return notFound();
                    }
                }
            });
    }

    /**
     *
     */
    @GET
    @Path("/{id}/parts/{part}/metadata/{scope}")
    @Produces("application/json")
    public Response getPartJson(@Context HttpServletRequest request,
                                @PathParam("id") final String blobID,
                                @PathParam("part") final String partName,
                                @PathParam("scope") final String scoper
        )
    {
        final ApiLogger logger = super.createApiLogger(request, "GET:/mcs/models/{id}/parts/{part}/metadata/{scope}", "mcsserver");
        final JsonScope scope  = getJsonScope(scoper);

        return super.restWrap(new RestWrap(logger)
            {
                public Response operation() throws Exception
                {
                    JSONObject json = catalogService.getPartMeta(XUUID.fromString(blobID), partName, scope);

                    if (json != null)
                        return ok(ConversionUtil.jsonToJson(json));
                    else
                        return notFound();
                }
            });
    }

    @GET
    @Path("/{id}/parts/{part}")
    @Produces("*/*")
    public Response getBlobData(@Context HttpServletRequest request,
                                @PathParam("id") final String blobID,
                                @PathParam("part") final String partName,
                                @Context final UriInfo uriInfo
        )
    {
        ApiLogger logger = super.createApiLogger(request, "GET:/mcs/models/{id}/data", "mcsserver");

        return super.restWrap(new RestWrap(logger)
            {
                public Response operation() throws Exception
                {
                    Blob blob = catalogService.getModelMeta(XUUID.fromString(blobID));

                    if (blob != null)
                    {
                        DownloadInfo download = catalogService.downloadPartData(blob, partName);

                        // i admit to disliking the implicit stuff JAX-RS does with the return value
                        if (download != null)
                            return okBuilder(download.data).type(download.mimeType).build();
                        else
                            return notFound("blob data", blobID);  // hackish
                    }
                    else
                    {
                        return notFound();
                    }
                }
            });
    }

    @DELETE
    @Path("/{id}/parts/{part}")
    @Produces("application/json")
    public Response deleteBlobPart(@Context HttpServletRequest request,
                                   @PathParam("id") final String blobID,
                                   @PathParam("part") final String partName,
                                   @Context final UriInfo uriInfo
        )
    {
        ApiLogger logger = super.createApiLogger(request, "DELETE:/mcs/models/{id}/data", "mcsserver");

        return super.restWrap(new RestWrap(logger)
            {
                public Response operation() throws Exception
                {
                    Blob blob = catalogService.getModelMeta(XUUID.fromString(blobID));

                    if (blob != null)
                    {
                        try
                        {
                            if (catalogService.deletePart(blob, partName))
                                return ok();
                            else
                                return notFound();
                        }
                        catch (LifecycleException lx)
                        {
                            return lifecycleProblem(lx);
                        }
                    }
                    else
                    {
                        return notFound();
                    }
                }
            });
    }

    /**
     * Searches science model metadata by "and"-ing together all the query parameters as name=value
     * search criteria.  Note that if an extra invalid n=v is included then and empty list will be
     * returned.
     */
    @GET
    @Produces("application/json")
    public Response searchBlobs(@Context HttpServletRequest request, @Context UriInfo uriInfo)
    {
        ApiLogger logger = super.createApiLogger(request, "GET:/mcs/models", "mcsserver");

        return super.restWrap(new RestWrap(logger)
            {
                public Response operation() throws Exception
                {
                    Map<String, List<String>> params = uriInfo.getQueryParameters();
                    Latest                    latest = getLatest(params);

                    if (latest != null)
                    {
                        ModelMeta meta = catalogService.searchBlobs(latest, makeQuery(uriInfo, true));

                        if (meta != null)
                            return ok(Arrays.asList(meta));
                        else
                            return ok(Collections.emptyList());
                    }
                    else
                    {
                        List<ModelMeta> metas = catalogService.searchBlobs(makeQuery(uriInfo, false));

                        return ok(metas);
                    }
                }
            });
    }

    /**
     * Ownership of logical IDs by blobs
     */
    @PUT
    @Path("{id}/owns")
    @Consumes("application/json")
    @Produces("application/json")
    public Response setOwnership(@Context HttpServletRequest request,
                                 @PathParam("id") String id,
                                 @Context UriInfo uriInfo,
                                 final List<String> logicalIDs
        )
    {
        ApiLogger logger = super.createApiLogger(request, "PUT:/mcs/models/{id}/owns", "mcsserver");

        return super.restWrap(new RestWrap(logger)
            {
                public Response operation() throws Exception
                {
                    Blob blob = catalogService.getModelMeta(XUUID.fromString(id));

                    if (blob != null)
                    {
                        catalogService.setOwner(blob, logicalIDs);

                        return ok();
                    }
                    else
                    {
                        return notFound();
                    }
                }
            });
    }

    @POST
    @Path("{id}/owns")
    @Consumes("application/json")
    @Produces("application/json")
    public Response addOwnership(@Context HttpServletRequest request,
                                 @PathParam("id") String blobID,
                                 @Context UriInfo uriInfo
        )
    {
        ApiLogger logger = super.createApiLogger(request, "POST:/mcs/models/{id}/owns", "mcsserver");

        throw new RuntimeException("Not yet supported");
        /*
        return super.restWrap(new RestWrap(logger) {
                public Response operation() throws Exception
                {
                    return ok();
                }});
        */
    }

    @GET
    @Path("{id}/owns")
    @Produces("application/json")
    public Response getOwnership(@Context HttpServletRequest request,
                                 @PathParam("id") String id,
                                 @Context UriInfo uriInfo
        )
    {
        ApiLogger logger = super.createApiLogger(request, "POST:/mcs/models/{id}/owns", "mcsserver");

        return super.restWrap(new RestWrap(logger)
            {
                public Response operation() throws Exception
                {
                    Blob blob = catalogService.getModelMeta(XUUID.fromString(id));

                    if (blob != null)
                        return ok(catalogService.getOwned(blob));
                    else
                        return notFound();
                }
            });
    }


    /**
     * Return the full logical meta of all dependencies of a model combination id
     */
    @GET
    @Path("{id}/deps/owners")
    @Produces("application/json")
    public Response getBlobDepsOwners(@Context HttpServletRequest request,
                                      @PathParam("id")    String id,
                                      @QueryParam("mode") String mode
        )
    {
        final ApiLogger logger = super.createApiLogger(request, "POST:/mcs/models/{id}/deps/owners", "mcsserver");
        final boolean   uses   = !("used-by").equals(mode);  // default is 'uses'
        final XUUID     xuuid  = XUUID.fromString(id);

        return super.restWrap(new RestWrap(logger)
            {
                public Response operation() throws Exception
                {
                    Blob              blob         = catalogService.getModelMeta(xuuid);
                    boolean           cacheDeps    = canCacheLogicalDeps(blob);
                    List<String>      owned        = catalogService.getOwned(blob);
                    List<LogicalMeta> logicalMetas = owned.stream()
                        .filter(logId -> logId != null)
                        .flatMap(logId -> catalogService.getMergedDependencies(logId, uses, cacheDeps).stream())
                        .collect(Collectors.toList());

                    return ok(logicalMetas);
                }
            });
    }

    /**
     * Sort of a hack to make it so that we cache logical dependencies for these lifecycle
     * states.  This is needed because the current flag on logical deps is only locked/unlocked
     * and it's set to locked only for RELEASED state.
     */
    private boolean canCacheLogicalDeps(Blob blob)
    {
        Lifecycle state = ConversionUtil.blobToModelMeta(blob).state;

        return ((state == Lifecycle.EVALUATED) ||
                (state == Lifecycle.ACCEPTED)  ||
                (state == Lifecycle.RELEASED));
    }

    /**
     * Returns Boolean.TRUE if method specifies replace, FALSE if it null or specifies update, or
     * null otherwise
     */
    private Boolean validateMethod(String method)
    {
        if ((method == null) || method.equals(METHOD_UPDATE))
            return Boolean.FALSE;
        else if (method.equals(METHOD_REPLACE))
            return Boolean.TRUE;
        else
            return null;
    }

    /**
     * Converts from exception to useful REST return
     */
    private Response lifecycleProblem(LifecycleException lx)
    {
        Operation op = lx.getOperation();

        if (op != null)
            return badRequest("Unable to perform operation " + op + " due to lifecycle state constraints");
        else
            return badRequest("Changing lifecycle state from " + lx.getTransitionFrom() + " to " + lx.getTransitionTo() + " is disallowed");
    }

    /**
     *
     */
    private Latest getLatest(Map<String, List<String>> params)
    {
        List<String> vals = params.get(QP_LATEST);

        if (vals != null)
        {
            String val = vals.get(0);

            if (val.equals(L_VERSION))
                return Latest.BY_VERSION;
            else if (val.equals(L_CREATION))
                return Latest.BY_CREATION_DATE;
            else if (val.equals(L_UPLOAD))
                return Latest.BY_LAST_UPLOAD_DATE;
        }

        return null;
    }

    /**
     * Handles conversion of the last component of metadata update URLs to JsonScope, throwing a
     * useful exception if it's not as expected
     */
    private JsonScope getJsonScope(String spec)
    {
        try
        {
            return JsonScope.valueOf(spec.toUpperCase());
        }
        catch (Exception x)
        {
            super.badRequest("Invalid JSON scope " + spec + "; must be 'core' or 'search'");  // throws exception

            return null;
        }
    }

    /**
     * Converts ALL n=v URL query parameters into a single BMS Query object that can be used to
     * search all science models.
     */
    private Query makeQuery(UriInfo uriInfo, boolean hasLatest)
    {
        QueryBuilder              qb      = new QueryBuilder();
        Map<String, List<String>> params  = uriInfo.getQueryParameters();
        List<Term>                ands    = new ArrayList<>();
        boolean                   inclDel = false;
        boolean                   inclArc = false;

        // note on "deleted"-ness:  by default soft-deleted blobs aren't returned in query results
        // as we don't want to litter the results with them.  this can be overridden by a query
        // param of "deleted=true" .

        // note on archival:  by default blobs with lifecycle state of ARCHIVED are not included
        // but this can be overridden by a query parameter of "archived=true".  note that
        // lifecycle state is orthogonal to soft-deleted flag

        qb.withPrefixes(JSON_QP_CORE, JSON_QP_SEARCH);  // first param is to use "extra_1" field of blobmgr

        if ((!hasLatest && params.size() > 0) || (hasLatest && params.size() > 1))
        {
            // the model to construct the BMS Query is a little awkward...

            for (Map.Entry<String, List<String>> entry : params.entrySet())
            {
                String       paramName = entry.getKey();
                List<String> values    = entry.getValue();
                boolean      arcTest   = paramName.equalsIgnoreCase(QP_INCLUDEARCHIVED);

                if (arcTest)
                    inclArc = getBooleanQpFlag(values);
                else if (Query.CoreField.DELETED == Query.isCore(paramName))
                    inclDel = getBooleanQpFlag(values);

                if (!arcTest && !(hasLatest && paramName.equals(QP_LATEST)))
                {
                    for (String entryVal : values)
                    {
                        OperAndArg opArg = getOpAndArg(entryVal);
                        Object     val   = untagValue(opArg.arg);

                        ands.add(qb.byGeneric(paramName, opArg.op, val));
                    }
                }
            }
        }

        if (!inclDel)
            ands.add(qb.byGeneric(Query.CoreField.DELETED.toString(), Query.Operator.EQ, Boolean.FALSE));

        ands.add(qb.byGeneric(McsMetadataDef.MD_STATE.getKeyName(),
                              ((inclArc) ? Query.Operator.EQ : Query.Operator.NEQ),
                              Lifecycle.ARCHIVED.toString()));

        // set root term to be an 'and' of all the name=value query params
        qb.term(qb.and(ands));

        return qb.build();
    }

    /**
     * Looks for tagging type (e.g., "long:" etc) on the value and converts the string form of
     * the value to the raw Java form.  Except for "date:" this is a pass-thru to central code
     */
    private static Object untagValue(String value)
    {
        if (value.startsWith("date:"))
        {
            Date dt;

            // "date:".length() == 5; format is mm/dd/yyyy
            if ((value.length() > 5) && ((dt = DateUtil.validateMMDDYYYY(value.substring(5))) != null))
                return dt;
        }

        return ConfigSetUtil.untagValue(value, false);
    }

    /**
     * Returns true if the first element of the list is "true", false otherwise
     */
    private boolean getBooleanQpFlag(List<String> values)
    {
        if (values.size() > 0)
            return Boolean.valueOf(values.get(0)).booleanValue();
        else
            return false;
    }

    /**
     * Process the value of a search query param by checking if it's actually an Operator, like so:
     * neq(avalue).  If it isn't a function-ish syntax then assume "eq" operator.
     */
    private OperAndArg getOpAndArg(String arg)
    {
        int lparen = arg.indexOf('(');

        if (lparen != -1)
        {
            if (!arg.endsWith(")"))
                throw new IllegalArgumentException("Expected closing ) in operator+arg " + arg);

            return new OperAndArg(Operator.valueOf(arg.substring(0, lparen).toUpperCase()),
                                  arg.substring(lparen + 1, arg.length() - 1));
        }
        else
        {
            return new OperAndArg(Operator.EQ, arg);
        }
    }

    private static boolean empty(String s)
    {
        return (s == null) || (s.length() == 0);
    }

}
