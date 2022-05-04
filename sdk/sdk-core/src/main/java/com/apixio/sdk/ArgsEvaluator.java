package com.apixio.sdk;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.apixio.sdk.Accessor.AccessorContext;
import com.apixio.sdk.protos.EvalProtos.AccessorCall;
import com.apixio.sdk.protos.EvalProtos.Arg;
import com.apixio.sdk.protos.EvalProtos.ArgList;
import com.apixio.sdk.protos.EvalProtos.Value;
import com.apixio.sdk.protos.FxProtos.FxType;

/**
 * Provides a separate context for evaluating an ArgList within the context of some set
 * of accessors, environment, and request.  The design assumes that the eval expression, the
 * list of accessors, and the environment are unchanged across requests.
 */
public class ArgsEvaluator
{

    /**
     * All info needed to evaluate an arglist against a specific request.
     * Params is optional and if it's present its size must be the same as
     * argsEval.getArgsList.size().
     */
    private ArgList              argsEval;
    private FxEnvironment        environment;
    private FxLogger             logger;
    private Map<String,Accessor> accessors;
    private List<FxType>         parameterTypes;

    /**
     * Accessors will be initialized with the given environment
     */
    public ArgsEvaluator(ArgList argsEval, FxEnvironment env, List<Accessor> accessors, List<FxType> parameterTypes) throws Exception
    {
        this.argsEval    = argsEval;
        this.environment = env;
        this.logger      = env.getLogger();

        if (parameterTypes != null)
        {
            if (parameterTypes.size() != argsEval.getArgsList().size())
                throw new IllegalStateException("Number of FxType declarations must be the same as the arg evaluator expressions:  " +
                                                parameterTypes.size() + " :: " + argsEval.getArgsList().size());

            this.parameterTypes = parameterTypes;
        }

        setupAccessors(accessors);
    }

    /**
     * Evaluate the pre-set arg list to get actual argument values.  If an argument is an accessor,
     * recursively evaluate its arguments, then call it, etc.
     */
    public List<Object> evaluate(FxRequest req, ArgList argsEvalIn) throws Exception
    {
        return evalArglist(req, argsEvalIn, -1);   // -1 means top level invocation so increment argNum
    }

    /**
     * Evaluate the pre-set arg list to get actual argument values.  If an argument is an accessor,
     * recursively evaluate its arguments, then call it, etc.
     */
    public List<Object> evaluate(FxRequest req) throws Exception
    {
        return evalArglist(req, argsEval, -1);   // -1 means top level invocation so increment argNum
    }

    /**
     * Recursively (as necessary) evaluate the args in the argument list and return the final
     * values in a List<Object> suitable for dynamically invoking a Method.
     */
    private List<Object> evalArglist(FxRequest request, ArgList arglist, int argNum) throws Exception
    {
        return evalArglist(request, arglist.getArgsList(), argNum);
    }

    /**
     * Evaluate each argument in the Arg list, making a recursive call if an arg value is another
     * accessor.  Note that we need to keep track of the top-level argument position as some accessors
     * need to make use of information about the formal declaration of such args (e.g., when restoring
     * data it needs to know what to convert it to).
     */
    private List<Object> evalArglist(FxRequest request, List<Arg> arglist, int argNum) throws Exception
    {
        List<Object> evaluatedArgs = new ArrayList<>();
        boolean      topArg        = (argNum < 0);

        // < 0 means this is the top-most iteration of args so we bump up arg number only in this
        // situation
        if (topArg)
            argNum = 0;

        for (Arg arg : arglist)
        {
            if (arg.getIsConst())
            {
                Value val = arg.getConstValue();

                switch (val.getValueCase())
                {
                    case INTVALUE:
                    {
                        evaluatedArgs.add(val.getIntValue());
                        break;
                    }
                    case LONGVALUE:
                    {
                        evaluatedArgs.add(val.getLongValue());
                        break;
                    }
                    case FLOATVALUE:
                    {
                        evaluatedArgs.add(val.getFloatValue());
                        break;
                    }
                    case DOUBLEVALUE:
                    {
                        evaluatedArgs.add(val.getDoubleValue());
                        break;
                    }
                    case BOOLEANVALUE:
                    {
                        evaluatedArgs.add(val.getBooleanValue());
                        break;
                    }
                    case STRINGVALUE:
                    {
                        evaluatedArgs.add(val.getStringValue());
                        break;
                    }
                }
            }
            else  // must be accessor call
            {
                AccessorCall    ac  = arg.getAccCall();
                Accessor        acc = getAccessor(ac.getAccessorName());
                AccessorContext ctx;

                ctx = new AccessorContext((parameterTypes != null) ? parameterTypes.get(argNum) : null,
                                          request);

                evaluatedArgs.add(acc.eval(ctx, evalArglist(request, ac.getArgsList(), argNum)));
            }

            if (topArg)
                argNum++;
        }

        return evaluatedArgs;
    }

    /**
     * Initializes all accessors with the configured environment
     */
    private void setupAccessors(List<Accessor> accessors) throws Exception
    {
        for (Accessor acc : accessors)
        {
            logger.info("Initializing accessor %s", acc.getID());
            acc.setEnvironment(environment);
        }

        this.accessors = accessors.stream().collect(Collectors.toMap(a -> a.getID(), a -> a));
    }

    /**
     * Return accessor by ID; it's an error if there isn't one by the given id
     */
    private Accessor getAccessor(String id)
    {
        Accessor acc = accessors.get(id);

        if (acc == null)
            throw new IllegalArgumentException("Attempt to use unknown accessor '" + id + '"');

        return acc;
    }
}
