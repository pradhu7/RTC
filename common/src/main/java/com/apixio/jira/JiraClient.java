package com.apixio.jira;

import java.util.Map;

public interface JiraClient {

    /**
     * Edit fields in a JIRA ticket. Fields parameter is a map of field key => value.
     * E.g. "customfield_14154" => 100
     * To see list of editable fields: GET https://apixio.atlassian.net/rest/api/latest/issue/<TICKET_ID>/editmeta
     * Returns the response from JIRA API
     */
    Map<String, Object> editJiraTicketFields(String ticketId, Map<String, Object> fields) throws Exception;

    /**
     * Transition the given JIRA ticket, transitionId should be obtained from the following:
     * GET https://apixio.atlassian.net/rest/api/latest/issue/<TICKET_ID>/transitions
     *
     * Current status can be obtained from {@link #getJiraTicketFields}.status.id
     */
    Map<String, Object> transitionJiraTicket(String ticketId, String transitionId) throws Exception;

    /**
     * Returns the fields response from JIRA API
     */
    Map<String, Object> getJiraTicketFields(String ticketId) throws Exception;
}
