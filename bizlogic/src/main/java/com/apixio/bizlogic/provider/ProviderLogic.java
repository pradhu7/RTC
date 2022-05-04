package com.apixio.bizlogic.provider;

import com.apixio.dao.provider.ProviderDAO;
import com.apixio.dao.provider.ValueAndIsClaimedCounts;
import com.apixio.dao.utility.DaoServices;
import com.apixio.dao.utility.sql.SqlOffsetIterator;
import com.apixio.dao.utility.sql.SqlOrdering;
import com.apixio.model.prospective.DistinctProviders;
import com.apixio.model.prospective.ProgramPerformance;
import com.apixio.model.prospective.ProviderPerformance;
import com.apixio.model.prospective.ProviderResponse;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collections;
import java.util.List;

/**
 * This class is used to read / write from the Mappings DB
 * Currently only supports the table provider_opps for pv/ca
 */
public class ProviderLogic {

    protected ProviderDAO providerDao;

    public ProviderLogic(final DaoServices daoServices) {
        this.providerDao = daoServices.getProviderDAO();
    }

    //Useful for tests
    public ProviderLogic(ProviderDAO dao) {
        this.providerDao = dao;
    }

    /**
     * Replace Into SQL a list of Provider Responses
     * Primary Key: ProjectId, code, patientId
     * Currently executes 1 insert at a time
     */
    public void insertRows(List<ProviderResponse> rows) {
        providerDao.insertProviderResponses(rows);
    }

    /**
     * Updates the fields subject to change for a provider response:
     *  DECISION
     *  NPI
     *  PROVIDER_GROUP
     *  PROVIDER_NAME
     *  IS_CLAIMED
     *  IS_DELIVERED
     *  DELIVERY_DATE
     *  RESPONSE_DATE
     */
    public void updateRows(List<ProviderResponse> rows) {
        providerDao.updateProviderResponses(rows);
    }


    public void deleteRows(List<ProviderResponse> rows) {
        for (ProviderResponse pv: rows) {
            providerDao.deleteProviderResponse(pv);
        }
    }

    /**
     * @param projectId Project Id
     * @param providerNames List of provider names to filter on or no filter if empty
     * @param providerGroups List of provider groups to filter on or no filter if empty
     * @param sortFields List of fields to sort on
     * @param pagination size of page
     * @param startPage starting page
     * @return iterator of provider responses
     */
    public SqlOffsetIterator<ProviderResponse> getProviderResponsesIterator(String projectId,
                                                                            List<String> providerNames,
                                                                            List<String> providerGroups,
                                                                            List<SqlOrdering> sortFields,
                                                                            int pagination,
                                                                            int startPage) {
        return providerDao.getProviderResponsesIterator(projectId, providerNames, providerGroups, sortFields, pagination, startPage);
    }

    /**
     * @param projectId Project Id
     * @param providerNames List of provider names to filter on or no filter if empty
     * @param providerGroups List of provider groups to filter on or no filter if empty
     * @param sortFields List of fields to sort on
     * @param pagination size of page
     * startPage is passed in as 0
     * @return iterator of provider responses
     */
    public SqlOffsetIterator<ProviderResponse> getProviderResponsesIterator(String projectId,
                                                                            List<String> providerNames,
                                                                            List<String> providerGroups,
                                                                            List<SqlOrdering> sortFields,
                                                                            int pagination) {
        return getProviderResponsesIterator(projectId, providerNames, providerGroups, sortFields, pagination, 0);
    }

    public List<ProgramPerformance> getProgramPerformance(String projectId,
                                                          List<String> providerNames,
                                                          List<String> providerGroups) {
        return providerDao.getProgramPerformance(projectId, providerNames, providerGroups);
    }

    /**
     * Get all Provider Responses for a list of NPIs within a project
     */
    public List<ProviderResponse> getNPIResponses(String projectId, List<String> npis) {
        return providerDao.getNPIResponses(projectId, npis);
    }

    /**
     * Retrieve an iterator for all providers and their respective groups starting at a specific "page"
     */
    public SqlOffsetIterator<DistinctProviders> getProvidersIterator(String projectId, int pagination, int startPage) {
        return providerDao.getProvidersIterator(projectId, pagination, startPage);
    }

    /**
     * Retrieve an iterator for all providers and their respective groups
     */
    public SqlOffsetIterator<DistinctProviders> getProvidersIterator(String projectId, int pagination) {
        return providerDao.getProvidersIterator(projectId, pagination, 0);
    }

    /**
     * Get all provider name and provider groups for a project
     * Iterators through distinct values 100 at a time
     */
    public List<DistinctProviders> getAllProviders(String projectId) {
        SqlOffsetIterator<DistinctProviders> iterator = getProvidersIterator(projectId, 100);
        List<DistinctProviders> providers = new java.util.ArrayList<>(Collections.emptyList());
        while (iterator.hasNext()) {
            providers.addAll(iterator.next());
        }
        return providers;
    }


    /////////////////////////// Provider Performance ////////////////////////////

    public SqlOffsetIterator<ProviderPerformance> providerPerformanceIterator(String projectId,
                                                                              List<String> providerNames,
                                                                              List<String> providerGroups,
                                                                              List<SqlOrdering> sortFields,
                                                                              int pagination,
                                                                              int startPage) {
        return providerDao.providerPerformanceIterator(projectId, providerNames, providerGroups, sortFields, pagination, startPage);
    }


    /////////////////////////// Program Performance ////////////////////////////

    /**
     * @param projectId project Id
     * @param providerNames List of provider names to filter on or no filter if empty
     * @param providerGroups List of provider groups to filter on or no filter if empty
     * @return Total number of opps
     */
    public Long totalOppsCount(String projectId, List<String> providerNames, List<String> providerGroups) {
        return providerDao.totalOppsCount(projectId, providerNames, providerGroups);
    }

    /**
     * @param projectId project Id
     * @param providerNames List of provider names to filter on or no filter if empty
     * @param providerGroups List of provider groups to filter on or no filter if empty
     * @return Total number of patients with opps
     */
    public Long totalPatientCount(String projectId, List<String> providerNames, List<String> providerGroups) {
        return providerDao.totalPatientCount(projectId, providerNames, providerGroups);
    }

    /**
     * @param projectId project Id
     * @param providerNames List of provider names to filter on or no filter if empty
     * @param providerGroups List of provider groups to filter on or no filter if empty
     * @return Number of opps for each disease category grouped by isClaimed
     */
    public List<ValueAndIsClaimedCounts> diseaseCategoryByClaimed(String projectId, List<String> providerNames, List<String> providerGroups) {
        return providerDao.diseaseCategoryByClaimed(projectId, providerNames, providerGroups);
    }

    /**
     * @param projectId project Id
     * @param providerNames List of provider names to filter on or no filter if empty
     * @param providerGroups List of provider groups to filter on or no filter if empty
     * @return Number of opps delivered or not grouped by isClaimed
     */
    public List<ValueAndIsClaimedCounts> deliveredByClaimed(String projectId, List<String> providerNames, List<String> providerGroups) {
        return providerDao.deliveredByClaimed(projectId, providerNames, providerGroups);
    }

    /**
     * @param projectId project Id
     * @param providerNames List of provider names to filter on or no filter if empty
     * @param providerGroups List of provider groups to filter on or no filter if empty
     * @param requireDelivery Option to only consider opps that have been delivered
     * @return Number of opps with or without a response grouped by isClaimed
     */
    public List<ValueAndIsClaimedCounts> respondedByClaimed(String projectId,
                                                            List<String> providerNames,
                                                            List<String> providerGroups,
                                                            boolean requireDelivery) {
        return providerDao.respondedByClaimed(projectId, providerNames, providerGroups, requireDelivery);
    }

    /**
     * default value of true for require delivery
     */
    public List<ValueAndIsClaimedCounts> respondedByClaimed(String projectId,
                                                            List<String> providerNames,
                                                            List<String> providerGroups) {
        return providerDao.respondedByClaimed(projectId, providerNames, providerGroups, true);
    }

    /**
     * @param projectId project Id
     * @param providerNames List of provider names to filter on or no filter if empty
     * @param providerGroups List of provider groups to filter on or no filter if empty
     * @param requireResponse Option to only consider opps that have a response
     * @return Number of opps grouped by decision and isClaimed
     */
    public List<ValueAndIsClaimedCounts> decisionsByClaimed(String projectId,
                                                            List<String> providerNames,
                                                            List<String> providerGroups,
                                                            Boolean requireResponse) {
        return providerDao.decisionsByClaimed(projectId, providerNames, providerGroups, requireResponse);
    }

    /**
     * default value of true for require response
     */
    public List<ValueAndIsClaimedCounts> decisionsByClaimed(String projectId,
                                                            List<String> providerNames,
                                                            List<String> providerGroups) {
        return providerDao.decisionsByClaimed(projectId, providerNames, providerGroups, true);
    }

    /**
     * @param projectId project Id
     * @param providerNames List of provider names to filter on or no filter if empty
     * @param providerGroups List of provider groups to filter on or no filter if empty
     * @return Sum of raf for all opps grouped by decision and isClaimed
     */
    public List<ValueAndIsClaimedCounts> rafByClaimedAndDecision(String projectId, List<String> providerNames, List<String> providerGroups) {
        return providerDao.rafByClaimedAndDecision(projectId, providerNames, providerGroups);
    }

    /**
     * @param projectId project Id
     * @param providerNames List of provider names to filter on or no filter if empty
     * @param providerGroups List of provider groups to filter on or no filter if empty
     * @return Sum of raf for all reportable opps grouped by isReportable and isClaimed
     */
    public List<ValueAndIsClaimedCounts> rafByClaimedAndReportable(String projectId, List<String> providerNames, List<String> providerGroups) {
        return providerDao.rafByClaimedAndReportable(projectId, providerNames, providerGroups);
    }
}


