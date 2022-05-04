package com.apixio.sdk;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import com.apixio.XUUID;
import com.apixio.mcs.RestClient;
import com.apixio.mcs.meta.McMeta.FullMeta;     // from .proto
import com.apixio.mcs.meta.McMeta.PartMeta;     // from .proto
import com.apixio.restbase.config.ConfigSet;
import com.apixio.sdk.protos.FxProtos;
import com.apixio.sdk.util.AllUriFetcher;
import com.apixio.sdk.util.CacheUtil;
import com.apixio.sdk.util.FxLoader;
import com.apixio.sdk.util.McsUtil;
import com.apixio.sdk.util.S3Util;
import com.apixio.sdk.util.ToolUtil;
import com.apixio.util.UriCacheManager;

/**
 * This class manages the operations needed to set up an f(x) implementation at runtime.
 *
 * There are two supported ways of setting up an f(x) impl: setting up from a local
 * directory that contains all elements of f(x) impl, and setting up from an MCID.
 *
 * Setup via directory requires at least two files:
 *
 *  * the implementation file; for JVMs this is a .jar and the file itself can be any name
 *  * the f(x) implementation information as recorded in protobuf form (FxImpl.pb format)
 *
 * The implementation can specify additional assets; these assets are referenced in the
 * implementation code via a name, and the actual file/URI is managed by the SDK (and
 * MCS).
 *
 * For setting up from a directory, the f(x) protobuf info MUST be in a file that has a
 * ".fxi" extension (for "fx implementation") as the setup code will search for files with
 * this extension.  Currently there can be only one such file.  That file is restored from
 * its protobuf form into FxImpl; the value of FxImpl.implurl is used to locate the file
 * with the same base name in the directory.  Additionally, the asset values in the asset
 * map are used to match with the remaining files in a similar way.
 *
 * For example, if the file "f2f.fxi" is restored to an FxImpl that has the following
 * fields:
 *
 *   implUrl: file:///tmp/facetoface-1.1.1.jar
 *   assets[0]: config -> file:///tmp/config.yaml
 *   assets[1]: model -> file:///tmp/model.zip
 *
 *  and the files in the directory used for initialization contains:
 *
 *    f2f.fxi
 *    facetoface-1.1.1.jar
 *    config.yaml
 *    model.zip
 *
 * then the setup will be successful and the assets map will be modified so the values are
 * referring to those files.
 *
 * For setting up from an MCID, the client caller needs to supply the XUUID and configuration;
 * this configuration will be used to create an MCS REST client (i.e., it has MCS server and
 * Apache HttpClient configuration) and a UriCacheManager (i.e., local directory to put cached
 * files in, and config for S3Ops construction to fetch from S3).
 */
public class Instantiator
{
    public final static String FXIMPL_EXTENSION = ".fxi";

    /**
     * ModelCombination Part names:
     *
     *  FXIMPL:          the name of the part that stores the serialized proto message FxImpl
     *  IMPLEMENTATION:  the name of the part that is the implementation
     */
    public final static String MCP_FXIMPL             = "FxImpl.pb";
    public final static String MCP_IMPLEMENTATION_JVM = "implementation.jar";

    /**
     * Sets up an FxImpl from the contents of a local FS directory.  This form of setup allows for
     * testing/use of the SDK without using the MCS server.  The directory must have exactly one
     * file that ends in .fxi (for FxImpl) that is the protobuf for the FxImpl to execute.  Each
     * URI referenced within that FxImpl will be shortened to just the name and each file referenced
     * by a URI must also be in that same directory--specifically, URI contents are not fetched
     * but must exist in the directory already.
     */
    public FxProtos.FxImpl setupFromDirectoryOrFile(File directoryOrFile) throws Exception
    {
        File                     directory = directoryOrFile.isDirectory() ? directoryOrFile: directoryOrFile.getParentFile();
        FxProtos.FxImpl          fxi       = findFxi(directoryOrFile);   // exception if not exactly 1 such file
        URI                      implUri   = findFile(directory, fxi.getImplUrl());
        FxProtos.FxImpl.Builder  builder   = FxProtos.FxImpl.newBuilder();

        if (implUri == null)
            throw new IllegalStateException("Missing implementation artifact in directory " + directory +
                                            " looking for " + fxi.getImplUrl());

        builder.setFxDef(fxi.getFxDef());
        builder.setEntryName(fxi.getEntryName());
        builder.setImplUrl(implUri.toURL().toString());

        for (Map.Entry<String,String> asset : fxi.getAssetsMap().entrySet())
        {
            String assetUrl = findFile(directory, asset.getValue()).toString();

            if (assetUrl == null)
                throw new IllegalStateException("Missing asset in directory " + directory + " looking for " + asset.getValue());

            builder.putAssets(asset.getKey(), assetUrl);
        }

        return builder.build();
    }

    /**
     * Sets up an FxImpl via an MC given by its ID.  This is done by fetching the metadata and parts of the MC
     * to a local directory, and then 
     *
     * Config must be parent of three related keys:
     *
     *   "mcs":    for accessing MC server
     *   "cache":  for caching URI objects
     *   "s3":     for accessing S3 objects
     */
    public FxProtos.FxImpl setupFromModelCombination(ConfigSet config, XUUID fxID,
                                                     String mcsServerOverride, String cacheDirOverride) throws Exception
    {
        RestClient               restClient = McsUtil.makeMcsUtil(new McsUtil.McsServerInfo(config, mcsServerOverride));
        FullMeta                 meta       = restClient.getServerMCMeta(fxID.toString());
        Map<String,URI>          partMap    = makePartMap(meta);
        FxProtos.FxImpl.Builder  builder    = FxProtos.FxImpl.newBuilder();
        UriCacheManager          uriCache   = CacheUtil.makeCacheManager(new CacheUtil.CacheInfo(config, cacheDirOverride),
                                                                new AllUriFetcher(
                                                                    S3Util.makeS3Ops(S3Util.S3Info.fromConfig(config)),
                                                                    null));
        FxProtos.FxImpl          fxi;

        //        System.out.println("SFM setupFromModelCombination mcmeta=" + meta);
        //        System.out.println("SFM setupFromModelCombination partMap=" + partMap);

        fxi = FxLoader.loadFxImpl(ToolUtil.readBytes(uriCache.getLocalObjectInfo(partMap.get(MCP_FXIMPL)).getPath().toFile()));

        //        System.out.println("SFM setupFromModelCombination fxi=" + fxi);

        builder.setFxDef(fxi.getFxDef());
        builder.setEntryName(fxi.getEntryName());

        for (Map.Entry<String,String> asset : fxi.getAssetsMap().entrySet())
        {
            String assetName = asset.getKey();
            URI    assetUri  = partMap.get(assetName);     // note that assetUri will be the original URI from the publishing yaml info

            //            System.out.println("SFM setupFromModelCombination assetName=" + assetName + ", url=" + asset.getValue() + ", uri=" + assetUri);

            if (assetUri == null)
                throw new IllegalStateException("Asset " + assetName + " is declared in FxImpl but isn't an MC part");
            else if (!assetUri.getScheme().equals("s3"))
                throw new IllegalStateException("Asset " + assetName + " has a non-S3 URI:  " + assetUri);

            builder.putAssets(assetName, uriCache.getLocalObjectInfo(assetUri).getPath().toUri().toString());
        }

        // impl url is *not* part of assetMap but *is* part of partMap
        // MCP_IMPLEMENTATION_JVM means this code can only instantiate JVM-based impls currently
        builder.setImplUrl(uriCache.getLocalObjectInfo(partMap.get(MCP_IMPLEMENTATION_JVM)).getPath().toString());

        return builder.build();
    }

    /**
     * Create map from MC part name to its URI.  There is one fixed name of MCP_FXIMPL
     */
    private Map<String,URI> makePartMap(FullMeta meta) throws Exception
    {
        Map<String,URI> map = new HashMap<>();

        // do it this way instead of streaming due to URI exception
        for (PartMeta pm : meta.getPartsList())
            map.put(pm.getName(), new URI(pm.getS3Path()));

        return map;
    }

    /**
     * Search for a single file in that directory that ends with FXIMPL_EXTENSION, load it
     * via protobuf and return it.
     */
    private FxProtos.FxImpl findFxi(File directoryOrFile) throws Exception  //!! yuk on Exception
    {
        String  ext  = FXIMPL_EXTENSION;
        File fxifile = null;
        if (directoryOrFile.getName().endsWith(FXIMPL_EXTENSION)) {
            fxifile = directoryOrFile;
        } else { 
            File[]  fxis = directoryOrFile.listFiles((dir, name) -> name.endsWith(ext));
            if (fxis.length != 1)
                throw new IllegalStateException("Looking for exactly 1 file ending with " + ext + " in directory " + directoryOrFile +
                                                " but found " + fxis.length + " instead");
            fxifile = fxis[0];
        }
        return FxLoader.loadFxImpl(ToolUtil.readBytes(fxifile));
    }

    /**
     * Search for a file in the directory whose name matches the last element of the given URL.  Return the
     * filepath as a URI as some downstream code needs URI form
     */
    private URI findFile(File directory, String url) throws Exception
    {
        // hacky-ish, just to get the filename
        String filename = Paths.get((new URL(url).toURI())).getFileName().toString();
        File[] match    = directory.listFiles((dir, name) -> name.equals(filename));

        if (match.length == 1)
            return match[0].toURI();
        else
            return null;
    }

}
