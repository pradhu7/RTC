package com.apixio.jira;

import com.google.api.client.auth.oauth.OAuthGetTemporaryToken;

class JiraOAuthGetTemporaryToken extends OAuthGetTemporaryToken {

    /**
     * @param authorizationServerUrl encoded authorization server URL
     */
    JiraOAuthGetTemporaryToken(String authorizationServerUrl) {
        super(authorizationServerUrl);
        this.usePost = true;
    }

}
