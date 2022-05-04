package com.apixio.dao.vault;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Interface for communication via REST with Vault server.  It also deals with
 * setup of the required Vault token.
 */
public class Vault
{
    private static final Logger LOGGER = LoggerFactory.getLogger(Vault.class);

    private static Base64.Decoder b64Decoder = Base64.getDecoder();

    /**
     * Names of bash env vars or Java system properties that pass access info from
     * outside to inside the JVM.  All Vault REST APIs other than authentication require
     * a vault token so either the token needs to be passed directly or the info for
     * approle-based authentication must be passed
     *
     * TOKEN_NAME1 and TOKEN_NAME2 are identical in purpose (used to retrieve a Vault
     * token from the environment) but if a value for TOKEN_NAME1 is found then the
     * code doesn't look at TOKEN_NAME2.
     */
    public final static String TOKEN_NAME1             = "APX_VAULT_TOKEN";
    public final static String TOKEN_NAME2             = "VAULT_TOKEN";           // this is what Vault cmdline looks for
    public final static String ROLEAUTH_ROLE_ID_NAME   = "APX_VAULT_ROLE_ID";
    public final static String ROLEAUTH_SECRET_ID_NAME = "APX_VAULT_SECRET_ID";

    /**
     * This is meant as a general mechanism to hold the scalar fields of the
     * "data" JSON object, which is contained in a metadata-like wrapper object.
     * Until it becomes cumbersome, it should just include the aggregate of
     * needed fields under "data".  Fields must be public to be populated by
     * jackson.
     */
    public static class DataField
    {
        public Map<String, String> data;  // for retrieving secrets
        public String client_token;
        public String ciphertext;
        public String plaintext;
        public String password;
    }

    /**
     * Generic response wrapper
     */
    static class VaultResponse
    {
        // all other top-level JSON fields are ignored but these
        public DataField auth;  // exists only in role auth response
        public DataField data;
    }

    /**
     * For kv-v1 only.  The structure of the kv-v2 response is data.data
     * (which ends up in DataField.data).  Sadly there's no easy way to allow
     * just configuration to specify kv-v1 vs kv-v2 due to URL path and return
     * structure differences.
     */
    static class VaultSecretResponse
    {
        public Map<String, String> data;
    }

    /**
     * Roleauth REST requires JSON struct like this
     */
    static class AuthParams
    {
        public String role_id;
        public String secret_id;

        AuthParams(String roleID, String secretID)
        {
            role_id   = roleID;
            secret_id = secretID;
        }
    }
        
    /**
     * Transit decryption requires JSON struct like this
     */
    static class TransitDecrypt
    {
        public String ciphertext;

        TransitDecrypt(String ciphertext)
        {
            this.ciphertext = ciphertext;
        }
    }

    /**
     * Ignore unknown so we can minimize classes as we really just need the various
     * "data" fields from the vault returned json
     */
    private static ObjectMapper objectMapper = ((new ObjectMapper()).
                                                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false));

    /**
     * fields
     */
    private String transitUrl;
    private String authUrl;
    private String kvUrl;

    /**
     * The all-important token.  Either it's initially specified via system props or env vars or it
     * comes via role/secret authentication to the vault server.  If we authenticate, then we
     * can refresh if a non-auth REST call to vault returns a 403.  This refresh can occur only once
     * per REST call.
     */
    private String token;
    private String roleID;
    private String secretID;

    /**
     * Transit path is the "mount point" of the transit subsystem of Vault.  By
     * default it is "transit" but must be supplied here.  It is used in the
     * URL for the HTTP endpoint that has the form
     *
     *  https://server:port/v1/{transitPath}/...
     *
     * datakeyname is an app-unique name that is the transit key.  As it ends up
     * in the URL the name cannot have "/" in it.  It shows up in the URL as
     *
     *  https://server:port/v1/{transitPath}/datakey/plaintext/{datakeyName}
     *
     * Both transitPath and secretsPath are the "mount points" of those engines in
     * vault and are taken directly from security config.
     */
    public Vault(String serverPort, String token, String transitPath, String secretsPath)
    {
        String v1Base = serverPort + "/v1";

        this.transitUrl = urlcat(v1Base, transitPath);
        this.kvUrl      = urlcat(v1Base, secretsPath);
        this.authUrl    = urlcat(v1Base, "auth/approle/login");  // see "» Login With AppRole" at https://www.vaultproject.io/api/auth/approle/index.html
        this.token      = token;                                 // this can be null, but must be set via setupToken() later
    }

    /**
     * Attempts to do a role-based authentication to get a vault token.
     * 
     * curl -s -X POST --data-binary \
     *   '{"role_id": "d0297393-86b4-b5b0-d450-f847c2defb9d","secret_id":"1db12547-0496-36dc-c574-0f855c56ef8b"}' \
     *   http://localhost:8200/v1/auth/approle/login
     *
     * The server response will have the following structure:
     *
     *  {
     *      "auth": {
     *          "accessor": "h1ppbrJ0DLb9IHDjWHkRMmbP",
     *          "client_token": "s.PqgsIvkgGThqXNTyorQ0LdkM",
     *          "entity_id": "506ee87f-f8da-b114-9826-e2875d98651d",
     *          "lease_duration": 86400,
     *          "metadata": {
     *              "role_name": "dev-role"
     *          },
     *          "policies": [
     *              "default"
     *          ],
     *          "renewable": true,
     *          "token_policies": [
     *              "default",
     *              "thepolicyname"                         <--- note: "thepolicyname" is just an example!
     *                                                      <--- the actual policy name MUST be returned or later calls will fail w/ 403
     *          ],
     *          "token_type": "service"
     *      },
     *      "data": null,
     *      "lease_duration": 0,
     *      "lease_id": "",
     *      "renewable": false,
     *      "request_id": "c2ce0bc6-6b6c-2caf-9228-20a5fc921348",
     *      "warnings": null,
     *      "wrap_info": null
     *  }
     *
     * and we're interested in auth.client_token.
     */
    private void fillTokenViaRoleAuth() throws RestException, IOException
    {
        if ((roleID == null) || (secretID == null))
            throw new IOException("Role-based authentication to Vault requires role and secret; check for system property or env" +
                                  ROLEAUTH_ROLE_ID_NAME + " and " + ROLEAUTH_SECRET_ID_NAME);

        AuthParams   auth = new AuthParams(roleID, secretID);
        RestResponse rawResponse;

        LOGGER.info("fillTokenViaRoleAuth " + roleID);

        rawResponse = (new Rest()
                       .url(authUrl)
                       .header("Content-Type", "application/json")
                       .body(objectMapper.writeValueAsString(auth).getBytes(StandardCharsets.UTF_8))
                       .post());

        LOGGER.info("fillTokenViaRoleAuth " + roleID + " got response code " + rawResponse.getStatus());

        if (rawResponse.getStatus() == 200)
        {
            String response = new String(rawResponse.getBody(), StandardCharsets.UTF_8);

            token = objectMapper.readValue(response, VaultResponse.class).auth.client_token;
        }
        else
        {
            throw new IOException("Vault request returned non-200 for url " + authUrl + " : " + rawResponse.getStatus());
        }
    }

    /**
     * Creates a new transit key.  A transit key is a new AES256 key (byte[32]) that can
     * be used to create datakeys (this is done by encrypting secure random key material with this
     * transit key).
     *
     * curl -v -X POST  -H "X-Vault-Token: $VAULT_TOKEN" -H \
     *      "Content-Type: application/json"
     *      --data-binary '{"type":"aes256-gcm96"}' $VAULT_ADDR/v1/$path_transit/keys/$keyname
     *
     * On success the server will NOT respond with content--it will have a 204 return code (no-content)
     */
    public void createTransitKey(String keyName, String type) throws RestException, IOException   //!! currently unused
    {
        Map<String, String> body = new HashMap<>();
        RestResponse        rawResponse;
        String              url;

        checkToken();

        body.put("type", type);  // as per https://www.vaultproject.io/api-docs/secret/transit

        // see "» Create Key" in https://www.vaultproject.io/api-docs/secret/transit
        url = transitUrl + "/keys/" + keyName;

        //        LOGGER.warn("createTransitkey URL=" + url);

        rawResponse = postWithRetry(url, objectMapper.writeValueAsString(body).getBytes(StandardCharsets.UTF_8));

        if (rawResponse.getStatus() != 204)
        {
            throw new IOException("Vault request returned non-204 for url " + url + " : " + rawResponse.getStatus() +
                                  " with body " + new String(rawResponse.getBody(), StandardCharsets.UTF_8));
        }
    }

    /**
     * Creates and returns a new datakey.  A datakey is a new AES256 key (byte[32]) encrypted with the
     * current transit key value.  The returned encrypted form of the value can be decrypted via the
     * normal transit decryption.
     *
     * curl -s -X POST -H "X-Vault-Token: $VAULT_TOKEN" $VAULT_ADDR/v1/$path_transit/datakey/plaintext/$datakeyname
     *
     * "plaintext" is the type; also can be "wrapped"
     *
     * The server response will have the following structure:
     *
     *  {
     *    "auth": null,
     *    "data": {
     *        "ciphertext": "vault:v2:5cImvetT4GiDoFOZuQit642y1nXsxe+Ipeil0/UNx0XW+t5QEC/jpk7FQ9exLNJvFPFoVhrXLDUZqauu",
     *        "plaintext": "G3JZP3TdqHBuFLZFgIEGF1whR7T80RZYpkMyEe5XtQM="
     *    },
     *    "lease_duration": 0,
     *    "lease_id": "",
     *    "renewable": false,
     *    "request_id": "e79d58b9-2ce4-14ac-b896-74942b21f830",
     *    "warnings": null,
     *    "wrap_info": null
     *  }
     *
     * and we're interested in just the "data" object/fields.  As plaintext is defined to be base64 encoding
     * of the AES key (if Vault setup for the given datakey is done correctly) we convert to byte[32] here.
     */
    public DataField createTransitDatakey(String datakeyName) throws RestException, IOException   //!! currently unused
    {
        RestResponse rawResponse;
        String       url;

        checkToken();

        // see "» Generate Data Key" in https://www.vaultproject.io/api/secret/transit/index.html
        url = transitUrl + "/datakey/plaintext/" + datakeyName;

        //        LOGGER.warn("createTransitDatakey URL=" + url);

        rawResponse = postWithRetry(url, null);

        if (rawResponse.getStatus() == 200)
        {
            String  response = new String(rawResponse.getBody(), StandardCharsets.UTF_8);

            return objectMapper.readValue(response, VaultResponse.class).data;
        }
        else
        {
            throw new IOException("Vault request returned non-200 for url " + url + " : " + rawResponse.getStatus() +
                                  " with body " + new String(rawResponse.getBody(), StandardCharsets.UTF_8));
        }
    }

    /**
     * General decryption of data via a transit key.  Specifically this is used to decrypt a datakey
     * that's been returned from getDatakey().  It is NOT used to decrypt application data (which would
     * be way too large); because we know it's limited this way, we can assume that the ciphertext
     * is the String that is just createTransitDatakey().ciphertext
     *
     * curl -s -X POST -H "X-Vault-Token: $VAULT_TOKEN" \
     *   -H "Content-Type: application/json" \
     *   --data-binary '{"ciphertext":"vault:v2:MaHwu/fxbNuxMzLNjw0jytLEihhPG/lV3Py7jPQtbt61JBm3ARADq5CgvyJdeCZCEd8EIoHCtG1kzeX2"}' \
     *   $VAULT_ADDR/v1/$path_transit/decrypt/$datakeyname
     *
     * The server response will have the following structure:
     *
     *  {
     *    "auth": null,
     *    "data": {
     *        "plaintext": "8TMP4NGR/p4D70kPoNul2w7GfnCOwK1vphJOIK6AeG4="
     *    },
     *    "lease_duration": 0,
     *    "lease_id": "",
     *    "renewable": false,
     *    "request_id": "81616205-a37c-fb16-c79e-93efc729abe8",
     *    "warnings": null,
     *    "wrap_info": null
     *  }
     *
     * and we're interested only in the "data" object/fields.
     */
    public byte[] decryptTransitValue(String datakeyName, String ciphertext) throws RestException, IOException
    {
        RestResponse rawResponse;
        String       url;
        String       param;

        checkToken();

        // see "» Decrypt Data" at https://www.vaultproject.io/api/secret/transit/index.html
        url = transitUrl + "/decrypt/" + datakeyName;

        param = objectMapper.writeValueAsString(new TransitDecrypt(ciphertext));

        //        LOGGER.warn("decryptTransitValue URL=" + url);

        rawResponse = postWithRetry(url, param.getBytes(StandardCharsets.UTF_8));

        if (rawResponse.getStatus() == 200)
        {
            String response = new String(rawResponse.getBody(), StandardCharsets.UTF_8);
            String base64   = objectMapper.readValue(response, VaultResponse.class).data.plaintext;

            return b64Decoder.decode(base64);
        }
        else
        {
            throw new IOException("Vault request returned non-200 for url " + url + " : " + rawResponse.getStatus() +
                                  " with body " + new String(rawResponse.getBody(), StandardCharsets.UTF_8));
        }
    }

    /**
     * General retrieval of a secret.  The system uses v1 of Key/Value secrets engine.
     *
     *   curl -S -X GET -H "X-Vault-Token: $VAULT_TOKEN" $VAULT_ADDR/v1/{secretsMount}/{name}
     *
     * {name} can have "/"s in it from all appearances (e.g., "apixio/v1").
     *
     * The server response will have the following structure:
     * 
     *   {
     *       "auth": null,
     *       "data": {
     *          "password": "puttherealoldpasswordhere",  <<-- "password" is the name of the key
     *          "version": "V01"                          <<-- a single kv name can have multiple k=v entries
     *       },
     *       "lease_duration": 2764800,
     *       "lease_id": "",
     *       "renewable": false,
     *       "request_id": "89ab6f0b-1a09-fec8-908e-1c2928c16bb8",
     *       "warnings": null,
     *       "wrap_info": null
     *   }
     *
     * and we're interested only in the "data" object/fields.
     */
    public Map<String, String> getSecret(String name) throws RestException, IOException
    {
        RestResponse rawResponse;
        String       url;

        checkToken();

        url = kvUrl + "/" + name;

        //        LOGGER.warn("getSecret URL=" + url);

        rawResponse = getWithRetry(url);

        //        LOGGER.warn("getSecret got response code " + rawResponse.getStatus());

        // there is a difference in kv-v1 and kv-v2 urls and structure of return value
        // v1 doesn't have "/data/" in the url and it doesn't have an extra "data" JSON
        // level as far as can be discerned.  kv-v2 has both.  this code works only
        // with kv-v1

        if (rawResponse.getStatus() == 200)
        {
            String  response = new String(rawResponse.getBody(), StandardCharsets.UTF_8);

            return objectMapper.readValue(response, VaultSecretResponse.class).data;
        }
        else
        {
            throw new IOException("Vault request returned non-200 for url " + url + " : " + rawResponse.getStatus() +
                                  " with body " + new String(rawResponse.getBody(), StandardCharsets.UTF_8));
        }
    }

    /**
     * General storing of a secret.  The system uses v1 of Key/Value secrets engine.
     *
     *   curl -S -X PUT -H "X-Vault-Token: $VAULT_TOKEN" $VAULT_ADDR/v1/{secretsMount}/{name}
     *
     * {name} can have "/"s in it from all appearances (e.g., "apixio/v1").
     *
     * The actual content that is PUT must be a JSON object with one or more "field":"value" pairs
     *
     * On success the server will NOT produce content but will have a 204 return code
     */
    public void putSecret(String name, Map<String, String> secret) throws RestException, IOException
    {
        RestResponse rawResponse;
        String       url;

        checkToken();

        url = kvUrl + "/" + name;
        //        LOGGER.warn("putSecret URL=" + url);

        rawResponse = putWithRetry(url, objectMapper.writeValueAsString(secret).getBytes(StandardCharsets.UTF_8));

        //        LOGGER.warn("putSecret got response code " + rawResponse.getStatus());

        if (rawResponse.getStatus() != 204)
        {
            throw new IOException("Vault request returned non-204 for url " + url + " : " + rawResponse.getStatus() +
                                  " with body " + new String(rawResponse.getBody(), StandardCharsets.UTF_8));
        }
    }

    /**
     * Performs role-based authentication as necessary to retrieve a token.  In
     * order to do this we need both the role ID and a secret ID.  The secret ID is
     * transitory in nature so this call needs to be made soon after getting the
     * secret.
     *
     * The role ID and secret ID can be passed from the invoking environment in one
     * of two ways: exported environment variables or Java system properties.  The
     * names of each are identical regardless of the source:
     *
     *  APX_VAULT_ROLE_ID
     *  APX_VAULE_SECRET_ID
     *
     * so the following JVM invocations are behaviorally identical:
     *
     *  $ export APX_VAULT_ROLE_ID=xyz
     *  $ export APX_VAULT_SECRET_ID=abc
     *  $ java -Dbcfipspath=... -Dbcprovpath=... some.apx.class
     *
     * and
     *
     *  $ java -DAPX_VAULT_ROLE_ID=xyz -DAPX_VAULT_SECRET_ID=abc -Dbcfipspath=... -Dbcprovpath=... some.apx.class
     *
     * Java system properties take precedence over environment variables.  Values are NOT trimmed
     */
    public void setupToken() throws RestException, IOException
    {
        // prefer APX_VAULT_TOKEN over VAULT_TOKEN:
        if ((token = getEnvOrProp(TOKEN_NAME1)) == null)
            token = getEnvOrProp(TOKEN_NAME2);

        if ((token == null) || token.isEmpty())
        {
            roleID   = getEnvOrProp(ROLEAUTH_ROLE_ID_NAME);
            secretID = getEnvOrProp(ROLEAUTH_SECRET_ID_NAME);

            if ((roleID == null) || roleID.isEmpty())
                missingParam("role-id", ROLEAUTH_ROLE_ID_NAME);
            else if ((secretID == null) || secretID.isEmpty())
                missingParam("secret-id", ROLEAUTH_SECRET_ID_NAME);

            fillTokenViaRoleAuth();
        }
    }

    /**
     * Centralize HTTP get/post/put code so that on a 403 we can attempt reauthentication followed
     * by another attempt.
     */
    private RestResponse getWithRetry(String url) throws RestException, IOException
    {
        Rest         request  = makeAuthenticatedRequest(url);
        RestResponse response = request.get();

        if (response.getStatus() == 403)
        {
            remakeAuthenticatedRequest(request);
            response = request.get();
        }

        return response;
    }

    private RestResponse postWithRetry(String url, byte[] body) throws RestException, IOException
    {
        Rest         request  = makeAuthenticatedRequest(url);
        RestResponse response;

        if (body != null)
            request.body(body);

        response = request.post();

        if (response.getStatus() == 403)
        {
            remakeAuthenticatedRequest(request);
            response = request.post();
        }

        return response;
    }

    private RestResponse putWithRetry(String url, byte[] body) throws RestException, IOException
    {
        Rest         request  = makeAuthenticatedRequest(url).body(body);
        RestResponse response = request.put();

        if (response.getStatus() == 403)
        {
            remakeAuthenticatedRequest(request);
            response = request.put();
        }

        return response;
    }

    /**
     * "Authenticated" is just a Rest object with X-Vault-Token header with the current token.
     * Remaking it means to attempt reauthentication and updating that header with the new token.
     */
    private Rest makeAuthenticatedRequest(String url)
    {
        Rest rest = new Rest();

        rest.url(url).header("X-Vault-Token", token);

        return rest;
    }

    private void remakeAuthenticatedRequest(Rest request) throws RestException, IOException
    {
        LOGGER.info("Vault REST call returned 403 - attempting reauthentication");

        fillTokenViaRoleAuth();
        request.header("X-Vault-Token", token);
    }

    private static String getEnvOrProp(String name)
    {
        String val = System.getenv(name);

        if (val != null)
            return val;
        else
            return System.getProperty(name);
    }

    private static void missingParam(String desc, String param)
    {
        throw new IllegalStateException("Missing role-based auth param '" + desc + "':  " +
                                        "export environment variable " + param + " or " +
                                        "declare JVM system property via -D" + param);
    }

    /**
     *
     */
    private void checkToken()
    {
        if (token == null)
            throw new IllegalStateException("Null vault token.  Token must be supplied during construction " +
                " or fillTokenViaRoleAuth must be called prior to other calls");
    }

    private static String urlcat(String... components)
    {
        StringBuilder sb = new StringBuilder();

        for (String comp : components)
        {
            comp = comp.trim();

            if (comp.length() == 0)
                throw new IllegalArgumentException("URL component is empty string!");

            while (comp.startsWith("/"))
                comp = comp.substring(1);

            while (comp.endsWith("/"))
                comp = comp.substring(0, comp.length() - 1);

            if (sb.length() > 0)
                sb.append("/");

            sb.append(comp);
        }

        return sb.toString();
    }

}
