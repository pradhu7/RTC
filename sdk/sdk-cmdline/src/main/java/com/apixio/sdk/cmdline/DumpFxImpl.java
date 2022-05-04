package com.apixio.sdk.cmdline;

import java.io.File;

import com.apixio.sdk.util.FxLoader;
import com.apixio.sdk.util.ToolUtil;

/**
 * Usage:
 *
 *  $ DumpFxImpl {filepath} ...
 *
 * Reads and displays the restored FxProto.FxImpl objects
 */
public class DumpFxImpl extends Cmdbase
{

    public static void main(String... args) throws Exception
    {
        for (String path : args)
        {
            System.out.println("################ " + path);
            System.out.println(FxLoader.loadFxImpl(ToolUtil.readBytes(new File(path))));
        }
    }

}
