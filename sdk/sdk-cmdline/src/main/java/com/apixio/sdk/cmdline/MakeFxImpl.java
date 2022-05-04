package com.apixio.sdk.cmdline;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import com.apixio.sdk.protos.FxProtos;   // protoc-generated
import com.apixio.sdk.util.FxBuilder;
import com.apixio.sdk.util.FxLoader;
import com.apixio.sdk.util.ToolUtil;

/**
 * Usage:
 *
 *  $ MakeFxImpl def={fxdefFilepath} \
 *     impl={implURL} entry={classname} out={fximplFilepath} \
 *     [assetName=URL, ...]
 *
 * where {idlParserClass} is the full Java classname of a class that implement IdlParser and whose
 * makeFxImple() method will be invoked with the result of reading and restoring {fxdefFilepath}.
 * The other args are used to create an FxImpl which is then serialized to the given output file path.
 */
public class MakeFxImpl extends Cmdbase
{

    private static class Args
    {
        String       fxDefPath;
        String       implUrl;
        String       entry;
        String       outPath;
        List<String> assets;

        Args(String fxDefPath, String implUrl, String entry, String outPath, List<String> assets)
        {
            this.fxDefPath   = fxDefPath;
            this.implUrl     = implUrl;
            this.entry       = entry;
            this.outPath     = outPath;
            this.assets      = assets;
        }

        @Override
        public String toString()
        {
            return ("args(" +
                    ", fxDefPath=" + fxDefPath +
                    ", implUrl=" + implUrl +
                    ", entry=" + entry +
                    ", outPath=" + outPath +
                    ", assets=" + assets +
                    "):");
        }
    }


    public static void main(String... args) throws Exception
    {
        MakeFxImpl cmd = new MakeFxImpl();
        Args       pargs;

        if ((pargs = cmd.parseArgs(args)) == null)
        {
            usage();
        }
        else
        {
            Map<String,String> assets = cmd.parseAssets(pargs.assets);
            FxProtos.FxDef     def    = FxLoader.loadFxDef(ToolUtil.readBytes(new File(pargs.fxDefPath)));
            FxProtos.FxImpl    impl   = FxBuilder.makeFxImpl(def, new URL(pargs.implUrl), pargs.entry, assets);

            ToolUtil.saveBytes(pargs.outPath, impl.toByteArray());
        }
    }

    private Args parseArgs(String[] args)
    {
        List<String> largs = toModifiableList(args);

        try
        {
            return new Args(
                require(largs, "def"),
                require(largs, "impl"),
                require(largs, "entry"),
                require(largs, "out"),
                largs);                   // leftovers
        }
        catch (IllegalArgumentException x)
        {
            System.err.println(x.getMessage());

            return null;
        }
    }

    private Map<String,String> parseAssets(List<String> rawAssets)
    {
        Map<String,String> assets = new HashMap<>();

        for (String a : rawAssets)
        {
            String[] parts = a.split("=");

            if (parts.length != 2)
                throw new IllegalArgumentException("name=url expected for asset string " + a);

            assets.put(parts[0], parts[1]);
        }

        return assets;
    }

    private static void usage()
    {
        System.out.println("Usage:  MakeFxImpl \\\n" +
                           " def={fxdefFilepath}     \\\n" +
                           " impl={implURL} entry={classname} out={fximplFilepath} \\\n" +
                           "    [assetName=assetURL,...]");
    }

}
