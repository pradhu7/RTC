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
 * Provides a Java interface into the RESTful UserAcct (remote) service.
 */
public class UserAcctProxy extends ProxyBase {

    private final String API_GET_ALL_USERS = makeUserAcctApiUrl("/users");
    private final String API_MODIFY_USER   = makeUserAcctApiUrl("/users/{userID}");  // note that {userID} is not ever referenced by anything--just useful info

    /**
     * The data received (in JSON) for user information.
     */
    public static class UserInfo {
        public String       userID;
        public String       emailAddress;
        public String       accountState;
        public List<String> roles;
        public String       firstName;
        public String       lastName;
        public String       middleInitial;
        public String       dateOfBirth;
        public String       officePhone;
        public String       cellPhone;

        public String toString()
        {
            return ("[user ID=" + userID +
                    "; state=" + accountState +
                    "; roles=" + roles +
                    "]");
        }
    };

    /**
     * The data received (in JSON) for role information.
     */
    public static class RoleInfo {
        public String       roleID;
        public String       name;
        public String       description;
        public String       assignable;

        public String toString()
        {
            return ("[role ID=" + roleID +
                    "; name=" + name +
                    "; desc=" + description +
                    "; assignables=" + assignable +
                    "]");
        }
    };

    /**
     * Create a new UserAcctProxy
     */
    public UserAcctProxy(String tokenizerServer, String userAcctServer)
    {
        super(tokenizerServer, userAcctServer);
    }

    /**
     * Query for all users and return the array of UserInfo objects received
     */
    public UserInfo[] getAllUsers(String inToken) throws Exception
    {
        String intToken = getInternalToken(inToken);

        try
        {
            RestTemplate       restTemplate = new RestTemplate();
            HttpHeaders        headers      = makeHttpHeaderJson(intToken);
            HttpEntity<String> entity       = new HttpEntity<String>("", headers);

            return restTemplate.exchange(API_GET_ALL_USERS, HttpMethod.GET, entity, UserInfo[].class).getBody();
        }
        finally
        {
            returnInternalToken(inToken, intToken);
        }
    }

    /**
     * Send a request to modify the user's Roles.
     */
    public void modifyUserJson(String inToken, String userID, List<String> roles) throws Exception
    {
        String intToken = getInternalToken(inToken);

        try
        {
            RestTemplate       restTemplate = new RestTemplate();
            HttpHeaders        headers      = makeHttpHeaderJson(intToken);
            String             body         = toJSON(makeModifyUserParams(roles));
            HttpEntity<String> entity       = new HttpEntity<String>(body, headers);

            restTemplate.put(API_MODIFY_USER, entity, userID);
        }
        finally
        {
            returnInternalToken(inToken, intToken);
        }
    }

    /**
     * Due to limitations, the set of roles for a User must be specified as a comma-separated list
     * instead of the more intuitive JSON list of strings.  The method converts from a list of
     * role names to a CSV and stuffs it into the more general name=value format that the server
     * needs.
     */
    private Map<String, Object> makeModifyUserParams(List<String> roles)
    {
        Map<String, Object> params = new HashMap<String, Object>();
        StringBuilder       sb     = new StringBuilder();

        for (String role : roles)
        {
            if (sb.length() > 0)
                sb.append(",");
            sb.append(role);
        }

        params.put("roles", sb.toString());

        return params;
    }

}
