package com.apixio.useracct.cmdline;

import java.io.IOException;
import java.io.StringWriter;
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
 * Provides a Java interface into the RESTful UserAcct (remote) service.
 */
public class RoleSetsProxy extends ProxyBase {

    private final String API_GET_ALL_ROLESETS = makeUserAcctApiUrl("/rolesets");
    private final String API_GET_ROLESET      = makeUserAcctApiUrl("/rolesets/{nameID}");
    private final String API_SET_ROLE_PRIVS   = makeUserAcctApiUrl("/rolesets/{nameID}/{role}");
    private final String API_CREATE_ROLE      = makeUserAcctApiUrl("/rolesets/{nameID}");

    /**
     * Format of JSON received from:
     *
     *  GET:/rolesets
     *    
[  
   {  
      "id":"RS_4b2b39ca-39c8-4221-846c-02b3c3ba86cc",
      "type":"organization",
      "nameID":"System",
      "name":"System Roles",
      "description":"Roles for System-type organizations",

      "roles":[  
         {  
            "id":"R2_1f8103a0-c215-4c59-a2a4-c2af5f9d4733",
            "name":"ROOT",
            "privileges":[  
               {  
                  "forMember":true,
                  "operation":"ManageSystem",
                  "aclTarget":"*"
               },
               {  
                  "forMember":true,
                  "operation":"ManagePipeline",
                  "aclTarget":"*"
               },
               {  
                  "forMember":true,
                  "operation":"ManageOrganization",
                  "aclTarget":"*"
               },
               {  
                  "forMember":true,
                  "operation":"ManageUser",
                  "aclTarget":"*"
               }
            ]
         },
      ]
   },
]
     *
     *
     * Format of JSON sent to:
     *
     *  POST:/rolesets:
     *      {"type":"[organization, project]", "nameID":"...e.g. System...", "name":"...System...", "description":"...something..."}
     *
     *  POST:/rolesets/{nameID}:
     *      {"name": "...e.g. ROOT...", "description":"root is god", "privileges": [ ... ] }
     *
     *  PUT:/rolesets/{nameID}/{role}:
     *      [ {"forMember":true/false, "operation":"blah", "acltarget":"blah"}, ... ]
     */

    /**
     * The data received (in JSON) for user information.
     */
    public static class RolePrivilege {
        public Boolean forMember;
        public String  operation;
        public String  aclTarget;

        public RolePrivilege()
        {
        }

        public RolePrivilege(boolean forMember, String operation, String aclTarget)
        {
            this.forMember = forMember;
            this.operation = operation;
            this.aclTarget = aclTarget;
        }

        @Override
        public String toString()
        {
            return "[priv: forMember=" + forMember + "; operation=" + operation + "; aclTarget=" + aclTarget + "]";
        }
    }

    public static class RoleInfo {
        public String id;
        public String name;
        public String description;
        public List<RolePrivilege> privileges;
    }

    public static class RoleSetInfo {
        public String id;
        public String type;
        public String nameID;
        public String name;
        public String description;

        public List<RoleInfo> roles;

        public String toString()
        {
            return ("[" +
                    "]");
        }
    };

    /**
     * Create a new RoleSetsProxy
     */
    public RoleSetsProxy(String tokenizerServer, String userAcctServer)
    {
        super(tokenizerServer, userAcctServer);
    }

    /**
     * Query for all users and return the array of UserInfo objects received
     */
    public RoleSetInfo[] getAllRoleSets(String inToken) throws Exception
    {
        String intToken = getInternalToken(inToken);

        try
        {
            RestTemplate       restTemplate = new RestTemplate();
            HttpHeaders        headers      = makeHttpHeaderJson(intToken);
            HttpEntity<String> entity       = new HttpEntity<String>("", headers);

            return restTemplate.exchange(API_GET_ALL_ROLESETS, HttpMethod.GET, entity, RoleSetInfo[].class).getBody();
        }
        finally
        {
            returnInternalToken(inToken, intToken);
        }
    }

    /**
     * Query for one roleset
     */
    public RoleSetInfo getRoleSet(String inToken, String nameID) throws Exception
    {
        String intToken = getInternalToken(inToken);

        try
        {
            RestTemplate       restTemplate = new RestTemplate();
            HttpHeaders        headers      = makeHttpHeaderJson(intToken);
            HttpEntity<String> entity       = new HttpEntity<String>("", headers);

            return restTemplate.exchange(API_GET_ROLESET, HttpMethod.GET, entity, RoleSetInfo.class, nameID).getBody();
        }
        finally
        {
            returnInternalToken(inToken, intToken);
        }
    }

    public void createRole(String inToken, String rsNameID, String roleName, String roleDesc, List<RolePrivilege> privileges) throws IOException
    {
        // POST:/rolesets/{nameID}   json: {"name": "...e.g. ROOT...", "description":"root is god", "privileges": [ ... ] }
        String intToken = getInternalToken(inToken);

        try
        {
            RestTemplate       restTemplate = new RestTemplate();
            HttpHeaders        headers      = makeHttpHeaderJson(intToken);
            String             body         = makeCreateRoleBody(roleName, roleDesc, privileges);
            HttpEntity<String> entity       = new HttpEntity<String>(body, headers);

            restTemplate.postForLocation(API_CREATE_ROLE, entity, rsNameID);
        }
        finally
        {
            returnInternalToken(inToken, intToken);
        }
    }

    private String makeCreateRoleBody(String roleName, String roleDesc, List<RolePrivilege> privs) throws IOException
    {
        StringWriter         sw   = new StringWriter();
        Map<String, Object>  json = new HashMap<>();

        json.put("name",        roleName);
        json.put("description", roleDesc);
        json.put("privileges",  privs);

        objectMapper.writeValue(sw, json);

        return sw.toString();
    }

    public void setRolePrivileges(String inToken, String setID, String roleName, List<RolePrivilege> privileges) throws Exception
    {
        String intToken = getInternalToken(inToken);

        try
        {
            RestTemplate       restTemplate = new RestTemplate();
            HttpHeaders        headers      = makeHttpHeaderJson(intToken);
            String             body         = makeSetRolePrivsBody(privileges);
            HttpEntity<String> entity       = new HttpEntity<String>(body, headers);

            restTemplate.put(API_SET_ROLE_PRIVS, entity, setID, roleName);
        }
        finally
        {
            returnInternalToken(inToken, intToken);
        }
    }

    private String makeSetRolePrivsBody(List<RolePrivilege> privs) throws IOException
    {
        StringWriter sw = new StringWriter();

        objectMapper.writeValue(sw, privs);

        return sw.toString();
    }
}
