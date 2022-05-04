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
 * Provides a Java interface into the RESTful ACL-specific UserAcct (remote) service.
 */
public class AclFuncsProxy extends ProxyBase {

    private final String API_GET_ALL_OPERATIONS  = makeUserAcctApiUrl("/aclop");
    private final String API_ADD_OPERATION       = makeUserAcctApiUrl("/aclop");
    private final String API_DELETE_OPERATION    = makeUserAcctApiUrl("/aclop/{name}");
    private final String API_GET_ALL_ACCESSTYPES = makeUserAcctApiUrl("/aclat");
    private final String API_ADD_ACCESSTYPE      = makeUserAcctApiUrl("/aclat");
    private final String API_DELETE_ACCESSTYPE   = makeUserAcctApiUrl("/aclat/{name}");

    /**
     * The data received (in JSON) for ACL object information.
     */
    public static class OperationInfo {
        public String id;
        public String name;
        public String description;
        public String accessTypes;

        public String toString()
        {
            return ("[aclop ID=" + id +
                    "; name=" + name +
                    "; desc=" + description +
                    "]");
        }
    };

    public static class AccessTypeInfo {
        public String id;
        public String name;
        public String description;

        public String toString()
        {
            return ("[aclat ID=" + id +
                    "; name=" + name +
                    "; desc=" + description +
                    "]");
        }
    };

    /**
     * Create a new AclFuncsProxy
     */
    public AclFuncsProxy(String tokenizerServer, String userAcctServer)
    {
        super(tokenizerServer, userAcctServer);
    }

    /**
     * Query for all users and return the array of UserInfo objects received
     */
    public OperationInfo[] getAllOperations(String inToken) throws Exception
    {
        RestTemplate       restTemplate = new RestTemplate();
        HttpHeaders        headers      = makeHttpHeaderJson(inToken);
        HttpEntity<String> entity       = new HttpEntity<String>("", headers);

        return restTemplate.exchange(API_GET_ALL_OPERATIONS, HttpMethod.GET, entity, OperationInfo[].class).getBody();
    }

    /**
     * Add a new Operation
     */
    public void addOperation(String inToken, String name, String description) throws Exception
    {
        RestTemplate       restTemplate = new RestTemplate();
        HttpHeaders        headers      = makeHttpHeaderJson(inToken);
        String             body         = toJSON(makeOperationBody(name, description));
        HttpEntity<String> entity       = new HttpEntity<String>(body, headers);

        restTemplate.postForLocation(API_ADD_OPERATION, entity);
    }

    private Map<String, Object> makeOperationBody(String name, String desc)
    {
        Map<String, Object> body = new HashMap<String, Object>();

        body.put("name",        name);
        body.put("description", desc);

        return body;
    }

    /**
     * Remove an existing Operation
     */
    public void removeOperation(String inToken, String name) throws Exception
    {
        RestTemplate  restTemplate = new RestTemplate();
        HttpHeaders   headers      = new HttpHeaders();
        HttpEntity<?> entity;

        headers.add("Authorization", "Apixio " + inToken);

        entity = new HttpEntity<Object>(headers);

        restTemplate.exchange(API_DELETE_OPERATION, HttpMethod.DELETE, entity, String.class, name);
    }

    /**
     * Query for all access types and return the array of objects received
     */
    public AccessTypeInfo[] getAllAccessTypes(String inToken) throws Exception
    {
        RestTemplate       restTemplate = new RestTemplate();
        HttpHeaders        headers      = makeHttpHeaderJson(inToken);
        HttpEntity<String> entity       = new HttpEntity<String>("", headers);

        return restTemplate.exchange(API_GET_ALL_ACCESSTYPES, HttpMethod.GET, entity, AccessTypeInfo[].class).getBody();
    }

    /**
     * Add a new AccessType
     */
    public void addAccessType(String inToken, String name, String description) throws Exception
    {
        RestTemplate       restTemplate = new RestTemplate();
        HttpHeaders        headers      = makeHttpHeaderJson(inToken);
        String             body         = toJSON(makeAccessTypeBody(name, description));
        HttpEntity<String> entity       = new HttpEntity<String>(body, headers);

        restTemplate.postForLocation(API_ADD_ACCESSTYPE, entity);
    }

    private Map<String, Object> makeAccessTypeBody(String name, String desc)
    {
        Map<String, Object> body = new HashMap<String, Object>();

        body.put("name",        name);
        body.put("description", desc);

        return body;
    }

    /**
     * Remove an existing AccessType
     */
    public void removeAccessType(String inToken, String name) throws Exception
    {
        RestTemplate  restTemplate = new RestTemplate();
        HttpHeaders   headers      = new HttpHeaders();
        HttpEntity<?> entity;

        headers.add("Authorization", "Apixio " + inToken);

        entity = new HttpEntity<Object>(headers);

        restTemplate.exchange(API_DELETE_ACCESSTYPE, HttpMethod.DELETE, entity, String.class, name);
    }

}
