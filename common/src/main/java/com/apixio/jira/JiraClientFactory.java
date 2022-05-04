package com.apixio.jira;

public class JiraClientFactory {

    private static final String JIRA_HOME = "https://apixio.atlassian.net";

    /**
     * Constructs a new JiraClient. All the credentials should come from a secret config file.
     */
    public JiraClient newJiraClient(String secret, String consumerKey, String privateKey, String accessToken) {
        return new JiraClientImpl(JIRA_HOME, secret, consumerKey, privateKey, accessToken);
    }
}
