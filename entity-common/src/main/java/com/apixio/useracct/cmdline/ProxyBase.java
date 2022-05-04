package com.apixio.useracct.cmdline;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;


/**
 * Provides a Java interface into the RESTful (remote) service.
 */
public class ProxyBase {

    protected static ObjectMapper objectMapper = new ObjectMapper();

    private String tokenizerServer;
    private String userAcctServer;

    private String API_GETINTERNALTOKEN;
    private String API_DELETEINTERNALTOKEN;

    private static class TokenInfo {
        public String token;
    };

    protected ProxyBase(String tokenizerServer, String userAcctServer)
    {
        if ((tokenizerServer == null) || (userAcctServer == null))
            throw new IllegalArgumentException("Null value for tokenizerServer or userAcctServer is disallowed");

        this.tokenizerServer = tokenizerServer;
        this.userAcctServer  = userAcctServer;

        API_GETINTERNALTOKEN    = makeTokenizerApiUrl("/tokens");
        API_DELETEINTERNALTOKEN = makeTokenizerApiUrl("/tokens/{id}");
    }

    protected String makeTokenizerApiUrl(String api)
    {
        if (!api.startsWith("/"))
            throw new IllegalArgumentException("API string must start with leading '/'");

        return tokenizerServer + api;
    }

    protected String makeUserAcctApiUrl(String api)
    {
        if (!api.startsWith("/"))
            throw new IllegalArgumentException("API string must start with leading '/'");

        return userAcctServer + api;
    }

    /**
     * Converts the Map<> into its JSON equivalent.  The map values can be either a String or
     * a List of Strings.
     */
    protected String toJSON(Map<String, Object> params)
    {
        ObjectNode  node = objectMapper.createObjectNode();

        for (Map.Entry<String, Object> entry : params.entrySet())
        {
            String k = entry.getKey();
            Object v = entry.getValue();

            if (v instanceof String)
            {
                node.put(k, (String) v);
            }
            else if (v instanceof List)
            {
                List<String>  ar = (List<String>) v;
                ArrayNode     an = node.putArray(k);

                for (String e : ar)
                    an.add(e);
            }
            else
            {
                throw new IllegalArgumentException("Map value [" + v + "] is not supported:  only String and List<String> are supported");
            }
        }

        return node.toString();
    }

    /**
     * Make the Spring RestTemplate headers with an Authorization HTTP header (if authenticated)
     * and declaring that application/json is the content type.
     */
    protected HttpHeaders makeHttpHeaderJson(String token)
    {
        HttpHeaders headers = makeHttpHeader(MediaType.APPLICATION_JSON, token);

        headers.setAccept(Arrays.asList(new MediaType[] {MediaType.APPLICATION_JSON}));

        return headers;
    }

    /**
     * Make the Spring RestTemplate headers with an Authorization HTTP header (if authenticated)
     * and declaring that application/x-www-url-formencoded is the content type.
     */
    protected HttpHeaders makeHttpHeaderFormUrlEncoded(String token)
    {
        return makeHttpHeader(MediaType.APPLICATION_FORM_URLENCODED, token);
    }

    /**
     * Make the Spring RestTemplate headers with an Authorization HTTP header (if authenticated)
     * and declaring content type appropriately.
     */
    private HttpHeaders makeHttpHeader(MediaType type, String token)
    {
        HttpHeaders         headers = new HttpHeaders();
        String              val;

        if (token != null)
            headers.add("Authorization", "Apixio " + token);

        headers.setContentType(type);

        return headers;
    }

    /**
     * Get an internal token given an external one
     */
    protected String getInternalToken(String externalToken)
    {
        // POST:/tokens  => {"token":"..."}
        RestTemplate       restTemplate = new RestTemplate();
        HttpHeaders        headers      = makeHttpHeaderJson(externalToken);
        HttpEntity<String> entity       = new HttpEntity<String>("", headers);
        TokenInfo          intToken     = restTemplate.postForObject(API_GETINTERNALTOKEN, entity, TokenInfo.class);

        if (intToken != null)
            return intToken.token;
        else
            return null;
    }

    /**
     * Return the internal token
     */
    protected void returnInternalToken(String externalToken, String internalToken)
    {
        // DELETE:/tokens/{id}
        RestTemplate       restTemplate = new RestTemplate();
        HttpHeaders        headers      = makeHttpHeaderJson(externalToken);
        HttpEntity<String> entity       = new HttpEntity<String>("", headers);

        restTemplate.exchange(API_DELETEINTERNALTOKEN, HttpMethod.DELETE, entity, ((Class<?>) null), internalToken);
    }

}
