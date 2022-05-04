package com.apixio.useracct.cmdline;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * An HTTP proxy for authentication of a user.
 */
public class AuthProxy extends ProxyBase {

    private final String API_AUTH   = makeUserAcctApiUrl("/auths");
    private final String API_LOGOUT = makeUserAcctApiUrl("/auths/{id}");

    public static class Token {
        public String  token;
        public long    passwordExpiresIn;
        public Boolean needsNewPassword;
        public Boolean passwordExpired;
        public String  nonce;

        public String toString()
        {
            return "[" + token + "]";
        }
    };

    public AuthProxy(String tokenizerServer, String userAcctServer)
    {
        super(tokenizerServer, userAcctServer);
    }

    /**
     * Perform authentication and return a Token instance on success
     */
    public Token authenticate(String emailAddr, String password) throws Exception
    {
        RestTemplate                   restTemplate = new RestTemplate();
        HttpHeaders                    headers      = makeHttpHeaderFormUrlEncoded(null);
        MultiValueMap<String, String>  body         = makeAuthBody(emailAddr, password);
        HttpEntity<MultiValueMap<String, String>>  entity = new HttpEntity<MultiValueMap<String, String>>(body, headers);

        return restTemplate.postForObject(API_AUTH, entity, Token.class);
    }

    private MultiValueMap<String, String> makeAuthBody(String email, String pass)
    {
        MultiValueMap<String, String> body = new HttpHeaders();

        body.add("email",    email);
        body.add("password", pass);

        return body;
    }

    /**
     * Logout a logged in auth token.
     */
    public void logout(String exToken, String inToken) throws Exception
    {
        RestTemplate         restTemplate = new RestTemplate();
        HttpHeaders          headers      = makeHttpHeaderFormUrlEncoded(inToken);  // no data passed so no form encoding
        HttpEntity<String>   entity       = new HttpEntity<String>(headers);

        restTemplate.exchange(API_LOGOUT, HttpMethod.DELETE, entity, ((Class<?>) null), exToken);
    }

}
