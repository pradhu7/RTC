package com.apixio.restbase.apiacl.perm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.apixio.SysServices;
import com.apixio.XUUID;
import com.apixio.aclsys.buslog.AclLogic;
import com.apixio.restbase.apiacl.AccessResult;
import com.apixio.restbase.apiacl.ApiAcls.InitInfo;
import com.apixio.restbase.apiacl.ApiCheck;
import com.apixio.restbase.apiacl.CheckContext;
import com.apixio.restbase.apiacl.HttpRequestInfo;
import com.apixio.restbase.apiacl.MatchResults;
import com.apixio.restbase.apiacl.ProtectedApi;
import com.apixio.restbase.apiacl.RestEnforcer.BooleanOp;
import com.apixio.restbase.apiacl.model.ApiDef;
import com.apixio.restbase.apiacl.perm.exts.ConstExtractor;

/**
 * ApiCheckPerm captures all the info needed to perform an ACL permission check on a
 * specific API (defined via config).
 */
class ApiCheckPerm extends ApiCheck {

    /**
     * Names of configuration keys whose values give the info about how to extract
     * that info from a request.
     */
    private final static String FIELD_SUBJECT    = "subject";
    private final static String FIELD_OPERATION  = "operation";
    private final static String FIELD_OBJECT     = "object";

    /**
     * This is to handle the case where no configuration for one of the above FIELD_*
     * is supplied.
     */
    private final static String NULL_EXTRACTOR = ExtractorFactory.EXTR_NULL + ":";

    private static class ExtractorTuple {
        List<Extractor> subExtractor;
        List<Extractor> oprExtractor;
        List<Extractor> objExtractor;

        ExtractorTuple(List<Extractor> subExtractor, List<Extractor> oprExtractor, List<Extractor> objExtractor)
        {
            this.subExtractor = subExtractor;
            this.oprExtractor = oprExtractor;
            this.objExtractor = objExtractor;
        }
    }

    private List<ExtractorTuple> tests = new ArrayList<>();
    private BooleanOp            oper;

    /**
     * External services needed
     */
    private SysServices sysServices;
    private AclLogic    aclLogic;

    ApiCheckPerm(InitInfo initInfo, ProtectedApi api, SysServices sysServices, AclLogic aclLogic, BooleanOp op, List<Map<String, String>> configs)
    {
        ApiDef def = api.getApiDef();

        this.sysServices = sysServices;
        this.aclLogic    = aclLogic;
        this.oper        = op;

        for (Map<String, String> config : configs)
        {
            ExtractorTuple tuple;

            tuple = new ExtractorTuple(
                getExtractor(initInfo, def, config, FIELD_SUBJECT),
                getExtractor(initInfo, def, config, FIELD_OPERATION),
                getExtractor(initInfo, def, config, FIELD_OBJECT));

            // Detect "no such Operation" early if possible
            if ((tuple.oprExtractor.size() == 1) && (tuple.oprExtractor.get(0) instanceof ConstExtractor))
            {
                // ! Assumes that ConstExtractor.extract() just returns the config value
                if (sysServices.getOperations().findOperationByName(config.get(FIELD_OPERATION)) == null)
                    throw new IllegalArgumentException("API ACL config problem:  Operation for ACL [" + config + "] doesn't exist.");
            }

            tests.add(tuple);
        }
    }

    /**
     * Check permission for an actual request, represented by the HttpRequestInfo and httpEntity.
     */
    @Override
    public AccessResult checkPermission(CheckContext ctx, HttpRequestInfo info, MatchResults match, Object httpEntity) throws IOException
    {
        String       path = info.getPathInfo();
        AccessResult res  = AccessResult.ALLOW;

        // we must check things if entity is null as that means we're coming in from the top-level
        // filter; we must also check things if the path is not empty as that means that the
        // top level filter received NEED_ENTITY return (which ends up adding the HTTP headers
        // which are then made available via HttpRequestInfo object).

        if ((httpEntity == null) || (path != null))
        {
            String    method  = info.getMethod();

            // It's cheaper/better to deal with everything at the filter level but if one of the
            // extractors needs the actual HTTP entity, then we have to wait until JAX-RS gets
            // the request.

            if ((httpEntity == null) && tupleNeedsEntity(tests))
            {
                ctx.recordDetail("Check permission:  some extractor needs HTTP entity.");

                res = AccessResult.NEED_ENTITY;
            }
            else
            {
                for (ExtractorTuple tuple : tests)
                {
                    String  sub = extract(ctx, tuple.subExtractor, match, info, httpEntity);
                    String  opr = extract(ctx, tuple.oprExtractor, match, info, httpEntity);
                    String  obj = extract(ctx, tuple.objExtractor, match, info, httpEntity);

                    if (isEmpty(sub) || isEmpty(opr) || isEmpty(obj))
                    {
                        ctx.recordDetail("Check permission aborting call to AclLogic.hasPermission as at least one is empty; sub=[{}], opr=[{}], obj=[{}]", sub, opr, obj);
                        res = AccessResult.NOT_FOUND;
                    }
                    else
                    {
                        boolean allowed = aclLogic.hasPermission(XUUID.fromString(sub), opr, obj);

                        ctx.recordDetail("Check permission calling AclLogic.hasPermission(sub={}, opr={}, obj={}) => {}", sub, opr, obj, allowed);
                        ctx.recordAccess(allowed, ((allowed) ? null : "user has no rights on object"), sub, opr, obj);

                        res = (allowed) ? AccessResult.ALLOW : AccessResult.DISALLOW; // catch/log exception here

                        // short-circuit if possible
                        if ( ((oper == BooleanOp.AND) && !allowed) ||
                             ((oper == BooleanOp.OR)  &&  allowed) )
                            break;
                    }
                }
            }
        }

        ctx.recordDetail("Check permission returning {}", res);
        
        return res;
    }

    /**
     * Extract and return the data extracted.  As it's a chain of extractors, each extractor is called with the
     * extracted value from the previous extraction.
     */
    private String extract(CheckContext ctx, List<Extractor> extractors, MatchResults match, HttpRequestInfo info, Object httpEntity) throws IOException
    {
        Object cur = null;

        for (Extractor extractor : extractors)
            cur = extractor.extract(ctx, match, info, cur, httpEntity);

        return (cur != null) ? cur.toString() : null;
    }

    /**
     *
     */
    private boolean isEmpty(String s)
    {
        return (s == null) || (s.length() == 0);
    }

    /**
     * Returns true if any of the extractors needs the HTTP entity.
     */
    private boolean tupleNeedsEntity(List<ExtractorTuple> tuples)
    {
        for (ExtractorTuple tuple : tuples)
        {
            if (needEntity(tuple.subExtractor) || needEntity(tuple.oprExtractor) || needEntity(tuple.objExtractor))
                return true;
        }

        return false;
    }

    private boolean needEntity(List<Extractor> extractors)
    {
        for (Extractor x : extractors)
        {
            if (x.requiresHttpEntity())
                return true;
        }

        return false;
    }

    /**
     * Pull out the subject/operation/object extractor configuration and create the appropriate
     * Extractor.
     */
    private List<Extractor> getExtractor(InitInfo initInfo, ApiDef api, Map<String, String> configs, String name)
    {
        String config = configs.get(name);
        int    col;

        // if there is no declaration of config for the subj/op/obj, use null extractor
        if (config == null)
            config = NULL_EXTRACTOR;

        return ExtractorFactory.makeExtractor(initInfo, api, config);
    }

}
