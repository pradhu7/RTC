package com.apixio.sdk.builtin;

import java.net.URI;
import java.util.List;

import com.apixio.sdk.Accessor;
import com.apixio.sdk.FxEnvironment;
import com.apixio.sdk.FxExecEnvironment;
import com.apixio.sdk.FxExecutor;
import com.apixio.sdk.protos.FxProtos.FxType;
import com.apixio.sdk.util.TypeUtil;
import com.apixio.sdk.util.Util;

/**
 * RestoreAccessor reads data from the supplied data URI.  It performs conversion from the persisted
 * (protobuf) form to symbolic type if either the evaluation environment or the accessor parameters
 * specifies the type.
 *
 * The parameter list for "restore(...)" is:
 *
 *  parameter 0:  (string) a DataURI; required
 *  parameter 1:  (string) symtype; optional
 *
 * This accessor is sensitive to the top-level f(x) parameter type declaration and will use it,
 * if it's available, as the symtype to convert to.  The optional second parameter will
 * take precendence over this implicit type, however.
 */
public class RestoreAccessor implements Accessor
{
    private final static String ACCESSOR_NAME = "restore";

    private FxExecutor executor;

    @Override
    public void setEnvironment(FxEnvironment env)
    {
        executor = Util.checkExecEnvironment(env).getFxExecutor();
    }

    @Override
    public String getID()
    {
        return ACCESSOR_NAME;
    }

    @Override
    public Object eval(AccessorContext context, List<Object> args) throws Exception
    {
        Object arg;

        if (args.size() != 1)
            throw new IllegalStateException("'" + ACCESSOR_NAME + "' accessor expects 1 arg but got " + args.size());
        else if ((arg = args.get(0)) == null)
            throw new IllegalArgumentException("'" + ACCESSOR_NAME + "' accessor requires non-null arg");

        return executor.restoreOutput(new URI((String) arg),
                                      getParamType(args, context.getFxType()));
    }

    /**
     * 
     */
    private String getParamType(List<Object> args, FxType type)
    {
        if ((args.size() > 1) && (args.get(1) instanceof String))
        {
            return (String) args.get(1);
        }
        else if (type == null)
        {
            return null;
        }
        else
        {
            if (!TypeUtil.isSequenceOfContainer(type))
                throw new IllegalStateException("restore accessor can only restore lists of objects but is trying to restore " + type);

            // ugly dereference chain...
            return type.getSequenceInfo().getOfType().getContainerInfo().getName();
        }
    }

}
