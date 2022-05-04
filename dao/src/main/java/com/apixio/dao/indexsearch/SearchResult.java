package com.apixio.dao.indexsearch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The search results consist of a list of uuids and list of <key, column>.
 * The list of unique (patient) uuids are ordered in descending order of the score of each uuid.
 * The list of <key, column> is used as an offset to get more results of the same query.
 */
public class SearchResult
{
    public SearchResult(List<SearchHit> hits, int next, int total)
    {
        this.hits  = hits;
        this.next  = next;
        this.total = total;
    }

    public static class SearchHit implements Comparable<SearchHit>
    {
        public SearchHit(String uuid, int score, int matchCount)
        {
            this.uuid = uuid;
            this.score = score;
            this.matchCount = matchCount;
        }

        @Override
        public boolean equals(Object other)
        {
            if (other == this)
            {
                return true;
            }

            if (!(other instanceof SearchHit))
            {
                return false;
            }

            SearchHit that = (SearchHit) other;
            return (this.uuid.equals(that.uuid) && this.score == that.score);
        }

        @Override
        public int hashCode()
        {
            return (41 * (41 + uuid.hashCode()) + score);
        }

        // order highest to lowest (score and then uuid and then matchCount)
        @Override
        public int compareTo(SearchHit o)
        {
            if (score == o.score)
            {
                if (uuid.equals(o.uuid))
                    return Integer.compare(o.matchCount, matchCount);

                return o.uuid.compareTo(uuid);
            }

            return Integer.compare(o.score, score);
        }

        public String uuid;
        public int    score;
        public int    matchCount;

        @Override
        public String toString()
        {
            return ("[SearchHit: "+
                    "uuid=" + uuid +
                    "; score=" + score +
                    "; matchCount=" + matchCount +
                    "]");
        }
    }

    public List<SearchHit> hits;
    public int             next;
    public int             total;

    public static void main(String[] args)
    {
        List<SearchHit>  searchHits = new ArrayList<>();

        searchHits.add(new SearchHit("5", 5, 0));
        searchHits.add(new SearchHit("8", 8, 0));
        searchHits.add(new SearchHit("4", 4, 0));
        searchHits.add(new SearchHit("9", 9, 0));
        searchHits.add(new SearchHit("3", 4, 0));
        searchHits.add(new SearchHit("9", 9, 1));
        searchHits.add(new SearchHit("10", 10, 0));

        Collections.sort(searchHits);

        for (SearchHit sh: searchHits)
        {
            System.out.println(sh);
        }
    }
}
