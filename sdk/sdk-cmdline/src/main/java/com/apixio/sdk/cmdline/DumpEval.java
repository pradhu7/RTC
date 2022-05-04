package com.apixio.sdk.cmdline;

import java.io.File;

import com.apixio.sdk.util.EvalLoader;
import com.apixio.sdk.util.ToolUtil;

/**
 * Usage:
 *
 *  $ DumpEval {filepath} ...
 *
 * Reads and displays the restored EvalProto.ArgList objects
 */
public class DumpEval extends Cmdbase
{

    public static void main(String... args) throws Exception
    {
        for (String path : args)
        {
            System.out.println("################ " + path);
            System.out.println(EvalLoader.loadArgList(ToolUtil.readBytes(new File(path))));
        }
    }

}
