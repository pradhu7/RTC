package com.apixio.restbase.config;

import java.util.List;
import java.util.Map;

/**
 */
public class FilterConfig {

    private List<Map<String, Object>> requestFilters;
    private String[]                  filterUrlPattern;

    public void setRequestFilters(List<Map<String, Object>> requestFilters)
    {
        this.requestFilters = requestFilters;
    }

    public List<Map<String, Object>> getRequestFilters()
    {
        return requestFilters;
    }

    public void setFilterUrlPattern(String[] pattern)
    {
        this.filterUrlPattern = pattern;
    }

    public String[] getFilterUrlPattern()
    {
        return filterUrlPattern;
    }

    @Override
    public String toString()
    {
        return ("FilterConfig: "
            );
    }

}
