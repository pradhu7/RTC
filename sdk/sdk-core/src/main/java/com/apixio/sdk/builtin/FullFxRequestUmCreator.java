package com.apixio.sdk.builtin;
import java.net.URI;
import java.util.Map.Entry;
import java.util.Map;
import java.util.TreeMap;

import com.apixio.dao.apxdata.ApxDataDao.QueryKeys;
import com.apixio.sdk.DataUriManager;
import com.apixio.sdk.FxEnvironment;
import com.apixio.sdk.FxRequest;
import com.apixio.sdk.UmCreator;

/**
 * This class enables FxTest to operate in a mode where the Uri structure is not known in advance.
 * 
 * The dataURI and associated grouping ID will be based on ALL elements of FxRequest and will have the keys sorted when serializing.
 */
public class FullFxRequestUmCreator implements UmCreator {

    private FxEnvironment env;
    private String domain;

    @Override
    public void setEnvironment(FxEnvironment env) throws Exception {
        this.env = env;
        this.domain = env.getAttribute("apx.domain");
    }

    @Override
    public DataUriManager createUriManager() throws Exception {
        
        FullRequestApxQueryDataUriManager dum = new FullRequestApxQueryDataUriManager();
        dum.setEnvironment(env);
        return dum;
    }
 
    private class FullRequestApxQueryDataUriManager extends ApxQueryDataUriManager {
    
        /**
         *  Extract a grouping ID from a map of key/value pairs. Keys will be sorted.
         * return valid groupingID for the given dataURI.
         * @throws Exception
         */
        @Override
        public String getGroupingID(URI dataURI)
        {
            validateScheme(dataURI);

            try {
                QueryKeys qk = getQueryKeys(dataURI);
                return makeGroupingID(qk.getKeys());
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
        @Override
        public String makeGroupingID(FxRequest req) throws Exception {
            return makeGroupingID(req.getAttributes());
        }

        public String makeGroupingID(Map<String,? extends Object> attrs) {
            StringBuilder sb   = new StringBuilder();
            for ( Entry<String, Object> entry : new TreeMap<String,Object>(attrs).entrySet()) {
                if (sb.length() > 0)
                    sb.append("-");
                sb.append(entry.getKey());
                sb.append(":");
                sb.append(entry.getValue());
            }
            return sb.toString();
        }

        @Override
        public QueryKeys makeQueryKeys(FxRequest req) throws Exception {
            QueryKeys      qk   = new QueryKeys();
            for( Entry<String, String> entry: new TreeMap<>(req.getAttributes()).entrySet()) {
                qk.with(entry.getKey(), entry.getValue());
            }
            return qk;
        }

        @Override
        public URI makeDataURI(FxRequest req) throws Exception {
            // To ensure that that the grouping ID is formed in the right order, we need
            // to always process URI params in the same order
            TreeMap<String, String> attrs = new TreeMap<>(req.getAttributes());
            return makeDataURI(domain, attrs);
        }        
    }
}
