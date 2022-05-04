package com.apixio.sdk.cmdline;

import java.util.List;

import com.apixio.sdk.protos.EvalProtos;   // protoc-generated
import com.apixio.sdk.util.FxEvalParser;
import com.apixio.sdk.util.ToolUtil;

/**
 * Usage:
 *
 *  $ MakeEval eval={paramlist} out={filepath}
 *
 * where {paramlist} is the csv list of how to evaluate the arg values needed to invoke
 * some compatible f(x).  The resulting EvalProto.ArgList will be serialized to {filepath}
 */
public class MakeEval extends Cmdbase
{
    private static class Args
    {
        String eval;
        String outPath;

        Args(String eval, String outPath)
        {
            this.eval    = eval;
            this.outPath = outPath;
        }

        @Override
        public String toString()
        {
            return ("args(" +
                    "eval=" + eval +
                    ", outPath=" + outPath +
                    "):");
        }
    }


    public static void main(String... args) throws Exception
    {
        MakeEval cmd = new MakeEval();
        Args     pargs;

        if ((pargs = cmd.parseArgs(args)) == null)
        {
            usage();
        }
        else
        {
            EvalProtos.ArgList  arglist = FxEvalParser.parse(pargs.eval);

            ToolUtil.saveBytes(pargs.outPath, arglist.toByteArray());
        }
    }

    private Args parseArgs(String[] args)
    {
        List<String> largs = toModifiableList(args);

        try
        {
            return new Args(
                require(largs, "eval"),
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
        System.out.println("Usage:  MakeEval \\\n" +
                           " eval={IDL} out={filepath}");
    }

}
