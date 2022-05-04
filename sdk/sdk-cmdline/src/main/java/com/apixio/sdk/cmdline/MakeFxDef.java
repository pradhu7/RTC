package com.apixio.sdk.cmdline;

import java.util.List;

import com.apixio.sdk.protos.FxProtos;   // protoc-generated
import com.apixio.sdk.util.FxIdlParser;
import com.apixio.sdk.util.ToolUtil;

/**
 * Usage:
 *
 *  $ MakeFxDef idl={idlParserClass} out={filepath}
 *
 * where {idlParserClass} is the full Java classname of a class that implement IdlParser and whose
 * makeFxDef() method will be invoked.  The resulting FxDef will be serialized to {filepath}
 */
public class MakeFxDef extends Cmdbase
{
    private static class Args
    {
        String idl;
        String outPath;

        Args(String idl, String outPath)
        {
            this.idl     = idl;
            this.outPath = outPath;
        }

        @Override
        public String toString()
        {
            return ("args(" +
                    "idl=" + idl +
                    ", outPath=" + outPath +
                    "):");
        }
    }


    public static void main(String... args) throws Exception
    {
        MakeFxDef cmd = new MakeFxDef();
        Args       pargs;

        if ((pargs = cmd.parseArgs(args)) == null)
        {
            usage();
        }
        else
        {
            FxProtos.FxDef  def = FxIdlParser.parse(pargs.idl);

            ToolUtil.saveBytes(pargs.outPath, def.toByteArray());
        }
    }

    private Args parseArgs(String[] args)
    {
        List<String> largs = toModifiableList(args);

        try
        {
            return new Args(
                require(largs, "idl"),
                require(largs, "out"));
        }
        catch (IllegalArgumentException x)
        {
            System.err.println(x.getMessage());

            return null;
        }
    }

    private static void usage()
    {
        System.out.println("Usage:  MakeFxDef \\\n" +
                           " idl={IDL} out={fximplFilepath}");
    }

}
