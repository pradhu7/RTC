package com.apixio.dao.indexsearch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.apixio.model.indexing.IndexableNGram;
import com.apixio.model.indexing.IndexableType;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apixio.datasource.cassandra.LocalCqlCache;
import com.apixio.datasource.cassandra.CqlCrud;
import com.apixio.dao.customerproperties.CustomerProperties;
import com.apixio.dao.Constants;
import com.apixio.datasource.cassandra.ColumnAndValue;
import com.apixio.datasource.cassandra.CqlCache;
import com.apixio.utility.DataSourceUtility;
import com.apixio.utility.HashCalculator;

import com.apixio.dao.indexsearch.DemographicData.DemographicType;
import com.apixio.dao.indexsearch.SearchResult.SearchHit;

/**
*  The IndexSearchDAO allows the indexing and search of demographics data.
 *
 * Indexing could either be a batch job (such as indexing bunch of patient
 * demographics from the pipeline) or one patient at at time (such as front end
 * dropwizard client). If only one demographic data is indexed, "localBatch"
 * must be set to true.
 *
 * All indices are stored in a single Cassandra table. Each demographic data
 * is stored in two type of rows (a row specific to the type of data and in
 * a generic row)
 *
 * The cassandra table structure is as follows:
 * row: "orgID"_"type"_"hashedValue"
 * column: "score"_"uuid"
 * The "type" is either uuid, fn, mn, ln, aid, pid, and nt. ("nt" means no type)
 * "hashedValue" and "score" are calculated by the NgramUtils (apixio scala utility)
 *
 * There are two ways to search: pass a query string (a list of words separated by space)
 * or list of (type + query string)
 *
 */

public class IndexSearchDAO
{
    private static final Logger logger = LoggerFactory.getLogger(IndexSearchDAO.class);

    private static final int MAX_NGRAAM_SIZE = 20;

    private static final Map<DemographicType, Integer> typeToMinNgramLen = new HashMap<>();
    static
    {
        // typeToMinNgramLen.put(DemographicType.uuid, 4);
        typeToMinNgramLen.put(DemographicType.uuid, 12);

        // typeToMinNgramLen.put(DemographicType.pid,  4);
        typeToMinNgramLen.put(DemographicType.pid,  12);

        typeToMinNgramLen.put(DemographicType.fn,   2);
        typeToMinNgramLen.put(DemographicType.ln,   2);

        // date patterns: "yyyy/MM/dd" and "MM/dd/yyyy"
        // typeToMinNgramLen.put(DemographicType.bd,   4);
        typeToMinNgramLen.put(DemographicType.bd,   10);

        // typeToMinNgramLen.put(DemographicType.aid,  4);
        typeToMinNgramLen.put(DemographicType.aid,  6);
    }

    private static final Map<DemographicType, Integer> typeToMaxNgramLen = new HashMap<>();
    static
    {
        typeToMaxNgramLen.put(DemographicType.uuid, MAX_NGRAAM_SIZE);
        typeToMaxNgramLen.put(DemographicType.pid,  MAX_NGRAAM_SIZE);
        typeToMaxNgramLen.put(DemographicType.fn,   MAX_NGRAAM_SIZE);
        typeToMaxNgramLen.put(DemographicType.ln,   MAX_NGRAAM_SIZE);
        typeToMaxNgramLen.put(DemographicType.bd,   MAX_NGRAAM_SIZE);
        typeToMaxNgramLen.put(DemographicType.aid,  MAX_NGRAAM_SIZE);
    }

    private CqlCache cqlCache;
    private CqlCrud  cqlCrud;
    private String   indexCF;

    /**
     * Set for indexing from the pipeline
     * Don't set cqlCache if for indexing from dropwizard (one demographic at a time)
     *
     * @param cqlCrud
     */
    public void setCqlCrud(CqlCrud cqlCrud, boolean useThreadLocalCache)
    {
        this.cqlCrud  = cqlCrud;
        if (useThreadLocalCache)
            this.cqlCache = cqlCrud.getCqlCache();
    }

    public void setIndexCF(String indexCF)
    {
        this.indexCF = indexCF;
    }

    /**
     * Index the demographic data.
     *
     * @param demographic
     * @param orgID
     * @throws Exception
     */
    public void indexDemographics(DemographicData demographic, String orgID)
        throws Exception
    {
        orgID = CustomerProperties.normalizeOrgIdToLongFormat(orgID);

        LocalCqlCache localCqlCache = cqlCache != null ? null: getLocalCqlCache();

        if (demographic.indexUuid)
            indexSingleItem(demographic.uuid, DemographicType.uuid, demographic.uuid, orgID, localCqlCache);

        indexSingleItem(demographic.primaryId, DemographicType.pid, demographic.uuid, orgID, localCqlCache);

        for (String firstName : demographic.firstNames)
        {
            indexSingleItem(firstName, DemographicType.fn, demographic.uuid, orgID, localCqlCache);
        }

        for (String lastName : demographic.lastNames)
        {
            indexSingleItem(lastName, DemographicType.ln, demographic.uuid, orgID, localCqlCache);
        }

        for (String birthDate : demographic.birthDates)
        {
            indexSingleItem(birthDate, DemographicType.bd, demographic.uuid, orgID, localCqlCache);
        }

        for (String aId : demographic.alternateIds)
        {
            indexSingleItem(aId, DemographicType.aid, demographic.uuid, orgID, localCqlCache);
        }

        if (localCqlCache != null)
            localCqlCache.flush();
    }

    private LocalCqlCache getLocalCqlCache()
    {
        LocalCqlCache localCqlCache = new LocalCqlCache();
        localCqlCache.setBufferSize(0);
        localCqlCache.setDontFlush(true);
        localCqlCache.setCqlCrud(cqlCrud);

        return localCqlCache;
    }

    /**
     * Indexes a single item with String value and IndexableType that contains custom nGram logic.
     *
     * @param value String to index
     * @param type IndexableType that contains nGram min and max lengths
     * @param uuid
     * @param orgId
     * @throws Exception
     */
    public void indexItem(String value, IndexableType type, String uuid, String orgId) throws Exception {
        String orgIdNormalized = CustomerProperties.normalizeOrgIdToLongFormat(orgId);
        if (StringUtils.isBlank(value)) {
            return;
        } else {
            value = value.trim().toLowerCase();
        }

        List<ValueAndScore> valuesAndScores = generateNGrams(value, type);

        byte[] defaultValue = "x".getBytes("UTF-8");

        CqlCache localCqlCache = cqlCache != null ? cqlCache: getLocalCqlCache();

        for (ValueAndScore valueAndScore : valuesAndScores) {
            String key1   = prepareKey(valueAndScore.value, type, orgIdNormalized);
            String key2   = prepareKey(valueAndScore.value, IndexableType.NT, orgIdNormalized);
            String column = valueAndScore.score + Constants.singleSeparator + uuid;

            DataSourceUtility.saveRawData(localCqlCache, key1, column, defaultValue, indexCF);
            DataSourceUtility.saveRawData(localCqlCache, key2, column, defaultValue, indexCF);
        }
    }

    /**
     * Returns true if the word was already indexed
     *
     * @param word
     * @param uuid
     * @param type
     * @param orgID
     * @return
     */
    public boolean isIndexed(String word, String uuid, IndexableType type, String orgID)
    {
        orgID = CustomerProperties.normalizeOrgIdToLongFormat(orgID);

        try
        {
            if (word != null)
                word = word.trim().toLowerCase();

            if (word == null || word.isEmpty())
                return true; // means no need to index

            if (word.length() > type.getMaxNGramLength())
                word = word.substring(0, type.getMaxNGramLength());

            if (word.length() < type.getMinNGramLength())
                return true; // means no need to index

            String key = prepareKey(word, type, orgID);
            String column = "1000" + Constants.singleSeparator + uuid;

            return DataSourceUtility.readColumnValue(cqlCrud, key, column, indexCF) != null;
        }
        catch (Exception e)
        {
            return false;
        }
    }

    private void indexSingleItem(String word, DemographicType type, String uuid, String orgID, LocalCqlCache localCqlCache)
        throws Exception
    {
        if (word != null)
            word = word.trim().toLowerCase();

        if (word == null || word.isEmpty())
            return;

        List<ValueAndScore> valueAndScores = generateNGrams(word, type);

        byte[] value = "x".getBytes("UTF-8");

        for (ValueAndScore valueAndScore : valueAndScores)
        {
            String key1   = prepareKey(valueAndScore.value, type, orgID);
            String key2   = prepareKey(valueAndScore.value, DemographicType.nt, orgID);
            String column = valueAndScore.score + Constants.singleSeparator + uuid;

            if (localCqlCache == null)
            {
                DataSourceUtility.saveRawData(cqlCache, key1, column, value, indexCF);
                DataSourceUtility.saveRawData(cqlCache, key2, column, value, indexCF);
            }
            else
            {
                DataSourceUtility.saveRawData(localCqlCache, key1, column, value, indexCF);
                DataSourceUtility.saveRawData(localCqlCache, key2, column, value, indexCF);
            }
        }
    }

    /**
     * Search given a query string. The query string is a list of words separated by space.
     * The result is a list of search hits ordered by score and an offset for the next search.
     *
     * @param query
     * @param orgID
     * @param next
     * @param limit
     * @return
     * @throws Exception
     */
    public SearchResult searchDemographics(String query, String orgID, int next, int limit)
        throws Exception
    {
        return searchDemographics(Arrays.asList(new QueryAndType(query, DemographicType.nt)), orgID, next, limit);
    }

    /**
     * Search given a query string and a type. The query string is a list of words separated by space.
     * The result is a list of search hits ordered by score and an offset for the next search.
     *
     * @param queryAndTypes
     * @param orgID
     * @param next
     * @param limit
     * @return
     * @throws Exception
     */
    public SearchResult searchDemographics(List<QueryAndType> queryAndTypes,  String orgID, int next, int limit)
            throws Exception
    {
        orgID = CustomerProperties.normalizeOrgIdToLongFormat(orgID);

        List<String> keys = new ArrayList<>();

        for (QueryAndType queryAndType : queryAndTypes)
        {
            String[] split = queryAndType.query.split(" ");

            for (int i = 0; i < split.length; i++)
            {
                String q = split[i].trim();
                if (q.isEmpty()) continue;

                if (q.length() > MAX_NGRAAM_SIZE)
                    q = q.substring(0, MAX_NGRAAM_SIZE);

                String key = prepareKey(q.toLowerCase(), queryAndType.type, orgID);
                keys.add(key);
            }
        }

        return searchDemographics(keys, next, limit);
    }

    /**
     * Search given a list of keys, possible offset, and limit
     *
     * The search always starts from the first column
     *
     * @param keys
     * @param next
     * @param limit
     * @return
     * @throws Exception
     */
    private SearchResult searchDemographics(List<String> keys, int next, int limit)
        throws Exception
    {
        int max = 5000;   // get up to 5000 columns per row. Very safe number.

        List<SearchHit>  finalHits = new ArrayList<>();

        for (String key : keys)
        {
            SearchIterator    iterator = new SearchIterator(key, null, max);
            List<SearchHit>   hits     = dedupRow(iterator, max);

            for (SearchHit hit : hits)
            {
                SearchHit sh = findMatchingUuid(finalHits, hit);
                if (sh == null)
                {
                    finalHits.add(hit);
                }
                else
                {
                    sh.score += hit.score;
                    sh.matchCount++;
                }
            }
        }

        Collections.sort(finalHits); // in descending order

        int finalSize = finalHits.size();
        int upTo = (next + limit) > finalSize ? finalSize : next + limit;
        List<SearchHit> trimmedResults = next < 0 || next >= finalSize ? new ArrayList<SearchHit>() : finalHits.subList(next, upTo);

        return new SearchResult(trimmedResults, trimmedResults.isEmpty() ?  -1 : upTo, finalSize > 5000 ? -1 : finalSize);
    }

    private SearchHit findMatchingUuid(List<SearchHit> hits, SearchHit hit)
    {
        for (SearchHit sh : hits)
        {
            if (sh.uuid.equals(hit.uuid))
                return sh;
        }

        return null;
    }

    private List<SearchHit> dedupRow(SearchIterator iterator, int max)
    {
        List<SearchHit> hits = new ArrayList<>();
        List<String>    ids  = new ArrayList<>();

        int i = 0;
        while (i++ < max && iterator.hasNext())
        {
            String column = iterator.next();
            String uuid   = getIdFromColumn(column);
            int    score  = getScoreFromColumn(column);

            if (ids.contains(uuid))
                continue;

            ids.add(uuid);
            hits.add(new SearchHit(uuid, score, 1));
        }

        return hits;
    }

    /**
     * Returns true if the word was already indexed
     *
     * @param word
     * @param uuid
     * @param type
     * @param orgID
     * @return
     */
    public boolean isIndexed(String word, String uuid, DemographicType type, String orgID)
    {
        orgID = CustomerProperties.normalizeOrgIdToLongFormat(orgID);

        try
        {
            if (word != null)
                word = word.trim().toLowerCase();

            if (word == null || word.isEmpty())
                return true; // means no need to index

            if (word.length() > typeToMaxNgramLen.get(type))
                word = word.substring(0, typeToMaxNgramLen.get(type));

            if (word.length() < typeToMinNgramLen.get(type))
                return true; // means no need to index

            String key = prepareKey(word, type, orgID);
            String column = "1000" + Constants.singleSeparator + uuid;

            return DataSourceUtility.readColumnValue(cqlCrud, key, column, indexCF) != null;
        }
        catch (Exception e)
        {
            return false;
        }
    }

    private String prepareKey(String id, DemographicType type, String orgID)
        throws Exception
    {
        return orgID + Constants.singleSeparator + type + Constants.singleSeparator + HashCalculator.getFileHash(id.getBytes("UTF-8"));
    }

    private String prepareKey(String id, IndexableType type, String orgID) throws Exception {
        return orgID + Constants.singleSeparator + type.name().toLowerCase() + Constants.singleSeparator +
                HashCalculator.getFileHash(id.getBytes("UTF-8"));
    }

    private Integer getScoreFromColumn(String column)
    {
        String[] splits = column.split(Constants.singleSeparator);

        return Integer.valueOf(splits[0]);
    }

    private String getIdFromColumn(String column)
    {
        String[] splits = column.split(Constants.singleSeparator);

        return splits[1];
    }

    private class SearchIterator implements Iterator<String>
    {
        private String localCopy;
        private String rowKey;

        Iterator<ColumnAndValue> columnAndValue;

        @Override
        public boolean hasNext()
        {
            return columnAndValue.hasNext();
        }

        @Override
        public String next()
        {
            if (localCopy != null)
            {
                String copy = localCopy;
                localCopy = null;
                return copy;
            }

            return nextGut();
        }

        public String peek()
        {
            if (localCopy != null)
            {
                return localCopy;
            }

            localCopy = nextGut();
            return localCopy;
        }

        private String nextGut()
        {
            if (!hasNext())
                return null;

            try
            {
                String column = columnAndValue.next().column;

                return column;
            }
            catch (Exception e)
            {
                return null;
            }
        }

        @Override
        public void remove()
        {
            throw new UnsupportedOperationException("Column Family Iterators cannot remove columns");
        }

        private SearchIterator(String key, String startingColumn, int bufferSize) throws Exception
        {
            rowKey = key;

            if (startingColumn == null)
                columnAndValue = cqlCrud.getAllColumnsDesc(indexCF, key, bufferSize);
            else
                columnAndValue = cqlCrud.getAllColumnsDesc(indexCF, key, startingColumn, bufferSize);
        }
    }

    private List<ValueAndScore> generateNGrams(String word, IndexableNGram type) {
        if (word.length() > type.getMaxNGramLength()) {
            word = word.substring(0, type.getMaxNGramLength());
        }

        List<ValueAndScore> valuesAndScores = new ArrayList<>();

        for (int i = word.length(); i >= type.getMinNGramLength(); i--) {
            String value = word.substring(0, i);
            String score = String.format("%04d", i * 1000 / word.length());

            ValueAndScore valueAndScore = new ValueAndScore(value, score);
            valuesAndScores.add(valueAndScore);
        }
        return valuesAndScores;
    }

    private List<ValueAndScore> generateNGrams(String word, DemographicType type)
    {
        if (word.length() > typeToMaxNgramLen.get(type)) word = word.substring(0, typeToMaxNgramLen.get(type));

        List<ValueAndScore> valueAndScores = new ArrayList<>();

        int length = word.length();
        for (int l = length; l >= typeToMinNgramLen.get(type); l--)
        {
            String value = word.substring(0, l);
            String score = String.format("%04d", l * 1000 / word.length());

            ValueAndScore valueAndScore = new ValueAndScore(value, score);
            valueAndScores.add(valueAndScore);
        }

        return valueAndScores;
    }

    private static class ValueAndScore
    {
        public ValueAndScore(String value, String score)
        {
            this.value = value;
            this.score = score;
        }

        private String value;
        private String score;
    }

    public static void main(String[] args)
    {
        String id = "01234";


        if (id.length() > 20) id = id.substring(0, 20);

        System.out.println(id);

        List<ValueAndScore> valueAndScores = new ArrayList<>();

        System.out.println("java code");
        int length = id.length();
        for (int l = length; l >= 2; l--)
        {
            String value = id.substring(0, l);
            String score = String.format("%04d", l * 1000 / id.length());

            System.out.print("value: " + value);
            System.out.println(", score: " + score);

            ValueAndScore valueAndScore = new ValueAndScore(value, score);
            valueAndScores.add(valueAndScore);
        }
    }
}
