package com.apixio.jira;

import com.google.api.client.auth.oauth.OAuthParameters;
import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.jackson2.JacksonFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

class JiraClientImpl implements JiraClient {

    private final JiraOAuthClient jiraOAuthClient;
    private final String jiraHome;
    private final String secret;
    private final String consumerKey;
    private final String privateKey;
    private final String accessToken;

    JiraClientImpl(String jiraHome, String secret, String consumerKey, String privateKey, String accessToken) {
        this.jiraOAuthClient = new JiraOAuthClient(jiraHome);
        this.jiraHome = jiraHome;
        this.secret = secret;
        this.consumerKey = consumerKey;
        this.privateKey = privateKey;
        this.accessToken = accessToken;
    }

    @Override
    public Map<String, Object> editJiraTicketFields(String ticketId, Map<String, Object> fields) throws Exception {
        Map<String, Object> putData = new HashMap<>();
        putData.put("fields", fields);
        JsonHttpContent putContent = new JsonHttpContent(new JacksonFactory(), putData);

        OAuthParameters parameters = jiraOAuthClient.getParameters(accessToken, secret, consumerKey, privateKey);

        HttpResponse response = putRequestToUrl(parameters, jiraTicketUrl(ticketId), putContent);
        return parseResponse(response);
    }

    @Override
    public Map<String, Object> transitionJiraTicket(String ticketId, String transitionId) throws Exception {
        Map<String, Object> postData = new HashMap<>();
        Map<String, Object> transitionMap = new HashMap<>();
        transitionMap.put("id", transitionId);
        postData.put("transition", transitionMap);
        JsonHttpContent postContent = new JsonHttpContent(new JacksonFactory(), postData);

        OAuthParameters parameters = jiraOAuthClient.getParameters(accessToken, secret, consumerKey, privateKey);

        HttpResponse response = postRequestToUrl(parameters, jiraTicketUrl(ticketId, "/transitions"), postContent);
        return parseResponse(response);
    }

    @Override
    public Map<String, Object> getJiraTicketFields(String ticketId) throws Exception {
        OAuthParameters parameters = jiraOAuthClient.getParameters(accessToken, secret, consumerKey, privateKey);
        HttpResponse response = getResponseFromUrl(parameters, jiraTicketUrl(ticketId));
        return parseResponse(response);
    }

    private GenericUrl jiraTicketUrl(String ticketId) {
        return jiraTicketUrl(ticketId, null);
    }

    private GenericUrl jiraTicketUrl(String ticketId, String path) {
        String url = jiraHome + "/rest/api/latest/issue/" + ticketId;
        if (path != null) url += path;
        return new GenericUrl(url);
    }

    private static HttpResponse putRequestToUrl(OAuthParameters parameters, GenericUrl jiraUrl, HttpContent putContent) throws IOException {
        HttpRequestFactory requestFactory = new NetHttpTransport().createRequestFactory(parameters);
        HttpRequest request = requestFactory.buildPutRequest(jiraUrl, putContent);
        return request.execute();
    }

    private static HttpResponse postRequestToUrl(OAuthParameters parameters, GenericUrl jiraUrl, HttpContent postContent) throws IOException {
        HttpRequestFactory requestFactory = new NetHttpTransport().createRequestFactory(parameters);
        HttpRequest request = requestFactory.buildPostRequest(jiraUrl, postContent);
        return request.execute();
    }

    private static Map<String, Object> parseResponse(HttpResponse response) throws IOException {
        Scanner s = new Scanner(response.getContent()).useDelimiter("\\A");
        String result = s.hasNext() ? s.next() : "";
        if (result.isEmpty()) return new HashMap<>();
        return new JacksonFactory().fromString(result, Map.class);
    }

    private static HttpResponse getResponseFromUrl(OAuthParameters parameters, GenericUrl jiraUrl) throws IOException {
        HttpRequestFactory requestFactory = new NetHttpTransport().createRequestFactory(parameters);
        HttpRequest request = requestFactory.buildGetRequest(jiraUrl);
        return request.execute();
    }
}
