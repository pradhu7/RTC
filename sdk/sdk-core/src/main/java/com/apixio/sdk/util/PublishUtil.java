package com.apixio.sdk.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.apixio.XUUID;
import com.apixio.mcs.ModelMeta;
import com.apixio.mcs.RestClient;
import com.apixio.sdk.FxLogger;
import com.apixio.sdk.Instantiator;
import com.apixio.sdk.protos.FxProtos.FxDef;
import com.apixio.sdk.protos.FxProtos.FxImpl;
import com.apixio.sdk.util.McsUtil.McsServerInfo;
import com.apixio.sdk.util.PublishYaml.Asset;
import com.apixio.sdk.util.PublishYaml.FunctionDef;
import com.apixio.sdk.util.PublishYaml.Meta;
import com.apixio.util.UriCacheManager.ObjectInfo;
import com.apixio.util.UriCacheManager;

/**
 * PublishUtil performs all operations necessary to publish an f(x) implementation to the MCS
 * server.  The details of the f(x) impl are given in a .yaml file that is parsed by PublishYaml and
 * provides getters for the semantic units needed to publish.
 *
 * Publishing is done via MCS RestClient which needs (yaml-based) configuration for server and
 * Apache HttpClient details.  Additionally, this code will cache the asset objects fetched as part
 * of the publishing as many of these objects are remote and large.  Support for objects that are
 * protected by either S3 access/secret keys or HTTP Basic Auth is included (via the use of
 * UriCacheManager, which the client must configure properly).
 */
public class PublishUtil
{

    private FxLogger        logger;
    private RestClient      restClient;
    private UriCacheManager uriCacheManager;

    /**
     * Create an instance that will use the given UriCacheManager and the given MCS server
     * info.
     */
    public PublishUtil(FxLogger logger, UriCacheManager uriCacheManager, McsServerInfo serverInfo) throws Exception
    {
        this.logger          = logger;
        this.uriCacheManager = uriCacheManager;
        this.restClient      = McsUtil.makeMcsUtil(serverInfo);
    }

    /**
     * Publish a single f(x) implementation, whose details are given in the PublishYaml object.
     */
    public void publish(String creator, PublishYaml py) throws Exception
    {
        // pull down to cache all assets/blobs; use com.apixio.util.S3CacheManager for s3:// URIs
        // verify existence of FXID, pull it down?
        // create MC
        // add fxdef info; add this as optional metadata:
        //  * fxid & possibly fxid   (this was in FxImpl)
        //  * entry                  (this was in FxImpl)
        //  * asset list??           (this was in FxImpl); name of "implementation.jar" IS REQUIRED!
        // add parts ("assets" or "blobs" key)
        // add MC dependencies as given in meta.logicalDependencies

        Meta       meta   = py.getMeta();
        FxDef      fxDef  = getFxDef(meta.fxid, meta.fnDef);
        ModelMeta  mcMeta = makeModelCombination(creator, py, fxDef);
        String     mcID   = mcMeta.id.toString();

        //        System.out.println("fxDef: " + fxDef);

        for (Asset asset : py.getAssets())
            addPart(mcID, creator, asset);

        addDependencies(meta.logicalID, py.getDependencies());

        logger.info("MC meta: %s", restClient.getServerMCMeta(mcID));
    }

    /**
     * Pull FxDef from MCS, first doing a creation of it if it's defined by its IDL form.  Note that
     * republishing the IDL to MCS will just return the existing FXID.
     */
    private FxDef getFxDef(XUUID fxid, FunctionDef fnDef) throws Exception
    {
        if (fxid == null)
            fxid = restClient.createFxDef(fnDef.idl, fnDef.name, fnDef.creator, fnDef.description);

        return restClient.getFxDef(fxid);
    }

    /**
     * Creator is email address
     */
    private ModelMeta makeModelCombination(String creator, PublishYaml py, FxDef fxDef) throws Exception
    {
        Meta               meta   = py.getMeta();
        ModelMeta          mcMeta = createMcMeta(creator, py.getMeta(), py.getCore(), py.getSearch());
        String             impl;
        XUUID              mcID;
        FxImpl             fxImpl;
        Map<String,String> assetMap;

        // FxImpl will contain:
        //  * FxDef
        //  * entryName (since it has no other easy home)
        //
        // and the fximpl will be stored as a part on mcid; part name will be "FxImpl.pb".
        // when restored this FxImpl.pb will be dup'ed and the implURL and assetURLs will
        // be added

        mcID = restClient.createModelCombination(mcMeta);  // returns a filled in copy of meta which includes MCID

        mcMeta = restClient.getModelCombinationMeta(mcID);

        // create our own map of assets as we need to remove impl (as it's kept in FxImpl pb)
        assetMap = py.getAssets().stream().collect(Collectors.toMap(a -> a.name, a -> a.uri.toString()));

        // implementation URL is required.  note that a remove() is done as the definition of FxImpl is
        // that the asset map does *not* include the implementation url!
        impl = assetMap.remove(Instantiator.MCP_IMPLEMENTATION_JVM);         // !! this code can only publish JVM-based implementations currently
        if (impl == null)
            throw new IllegalStateException("YAML key 'assets' requires an '" + Instantiator.MCP_IMPLEMENTATION_JVM + "' element under it");

        fxImpl = FxBuilder.makeFxImpl(fxDef, new URL(impl), meta.entry, assetMap);

        // create fximpl pb, and attach as part
        restClient.createPart(mcID.toString(), Instantiator.MCP_FXIMPL, "application/x-protobuf",
                              new ByteArrayInputStream(fxImpl.toByteArray()), creator);

        // assign ownership of logicalName to the new MCID in case anything wants to
        // reference it as a dependency.  Note that while the underlying MCS model
        // allows a single MCID to own multiple logicalIDs, this level only supports
        // a one-to-one on ownership
        restClient.claimOwnershipOfLogicalIDs(mcID.toString(), Arrays.asList(meta.logicalID));

        return mcMeta;
    }

    /**
     *
     */
    private ModelMeta createMcMeta(String creator, Meta meta, Map<String,Object> core, Map<String,Object> search)
    {
        ModelMeta mcMeta = new ModelMeta();

        mcMeta.createdBy  = creator;
        mcMeta.executor   = meta.executor;
        mcMeta.outputType = meta.outputType;
        mcMeta.product    = meta.product;
        mcMeta.name       = meta.name;

        mcMeta.core       = core;
        mcMeta.search     = search;

        return mcMeta;
    }

    /**
     *
     */
    private void addPart(String mcID, String creator, Asset asset) throws Exception
    {
        ObjectInfo info = uriCacheManager.getLocalObjectInfo(asset.uri);

        if (restClient.findPart(info.getMd5Hash()) == null)
        {
            try (InputStream is = uriCacheManager.getObject(asset.uri))
            {
                restClient.createPart(mcID, asset.name, asset.mimeType, is, creator);
            }
        }
        else
        {
            logger.info("PublishUtil.addPart is reusing MCS part as there's a match on MD5:  %s", info);
            restClient.createPartByRef(mcID, asset.name, asset.mimeType, info.getMd5Hash(), creator);
        }
    }

    /**
     *
     */
    private void addDependencies(String leftLogicalID, List<String> rightLogicalIDs) throws Exception
    {
        if ((rightLogicalIDs != null) && (rightLogicalIDs.size() > 0))
            restClient.setLogicalDependencies(leftLogicalID, rightLogicalIDs);
    }

}
