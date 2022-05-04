package com.apixio.useracct.cmdline;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * Provides a Java interface into the RESTful PassPolicies (remote) service.
 */
public class PassPoliciesProxy extends ProxyBase {

    private final String API_GET_PASSPOLICIES = makeUserAcctApiUrl("/sys/passpolicies");
    private final String API_ADD_PASSPOLICY   = makeUserAcctApiUrl("/passpolicies");
    private final String API_GET_PASSPOLICY   = makeUserAcctApiUrl("/passpolicies/{policyName}");
    private final String API_MOD_PASSPOLICY   = makeUserAcctApiUrl("/passpolicies/{policyName}");

    /**
     * The data received (in JSON) for user information.
     */
    public static class PolicyInfo {
        public String       policyID;
        public String       name;
        public Integer      maxDays;
        public Integer      minChars;
        public Integer      maxChars;
        public Integer      minLower;
        public Integer      minUpper;
        public Integer      minDigits;
        public Integer      minSymbols;
        public Integer      noReuseCount;
        public Boolean      noUserID;

        public String toString()
        {
            return ("[policy ID=" + policyID +
                    "; name=" + name +
                    "; maxDays=" + maxDays +
                    "; minChars=" + minChars +
                    "; maxChars=" + maxChars +
                    "; minLower=" + minLower +
                    "; minUpper=" + minUpper +
                    "; minDigits=" + minDigits +
                    "; minSymbols=" + minSymbols +
                    "; noReuseCount=" + noReuseCount +
                    "; noUserID=" + noUserID +
                    "]");
        }
    };

    /**
     * Create a new PassPoliciesProxy
     */
    public PassPoliciesProxy(String tokenizerServer, String userAcctServer)
    {
        super(tokenizerServer, userAcctServer);
    }

    /**
     * Query for all users and return the array of UserInfo objects received
     */
    public PolicyInfo[] getAllPolicies(String inToken) throws Exception
    {
        String intToken = getInternalToken(inToken);

        try
        {
            RestTemplate       restTemplate = new RestTemplate();
            HttpHeaders        headers      = makeHttpHeaderJson(intToken);
            HttpEntity<String> entity       = new HttpEntity<String>("", headers);

            return restTemplate.exchange(API_GET_PASSPOLICIES, HttpMethod.GET, entity, PolicyInfo[].class).getBody();
        }
        finally
        {
            returnInternalToken(inToken, intToken);
        }
    }

    /**
     * Add a new role
     */
    public void addPolicy(String inToken, String policyName, Map<String, String> params) throws Exception
    {
        String intToken = getInternalToken(inToken);

        try
        {
            RestTemplate       restTemplate = new RestTemplate();
            HttpHeaders        headers      = makeHttpHeaderJson(intToken);
            String             body         = toJSON(makePolicyBody(policyName, params));
            HttpEntity<String> entity       = new HttpEntity<String>(body, headers);

            restTemplate.postForLocation(API_ADD_PASSPOLICY, entity);
        }
        finally
        {
            returnInternalToken(inToken, intToken);
        }
    }

    private Map<String, Object> makePolicyBody(String name, Map<String, String> params)
    {
        Map<String, Object> body = new HashMap<String, Object>();

        body.put("name", name);

        addBodyParams(body, params);

        return body;
    }

    /**
     * Modify a policy
     */
    public void modifyPolicy(String inToken, String policyName, Map<String, String> params) throws Exception
    {
        String intToken = getInternalToken(inToken);

        try
        {
            RestTemplate       restTemplate = new RestTemplate();
            HttpHeaders        headers      = makeHttpHeaderJson(intToken);
            String             body         = toJSON(makeModifyPolicyBody(params));
            HttpEntity<String> entity       = new HttpEntity<String>(body, headers);

            restTemplate.put(API_MOD_PASSPOLICY, entity, policyName);
        }
        finally
        {
            returnInternalToken(inToken, intToken);
        }
    }

    private Map<String, Object> makeModifyPolicyBody(Map<String, String> params)
    {
        Map<String, Object> body = new HashMap<String, Object>();

        addBodyParams(body, params);

        return body;
    }


    private static void addBodyParams(Map<String, Object> body, Map<String, String> params)
    {
        for (Map.Entry<String, String> entry : params.entrySet())
            body.put(entry.getKey(), entry.getValue());
    }
}
