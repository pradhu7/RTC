package com.apixio.dao.provider;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

//Example usage: select sum(raf) as count, diseaseCategory as value, isClaimed as bool from provider_opps;
public class ValueAndIsClaimedCounts {

    final public float count;
    final public String value;
    final public boolean isClaimed;

    public final static String FIELD_COUNT = "count";
    public final static String FIELD_VALUE = "value";
    public final static String FIELD_BOOL = "is_claimed";

    public ValueAndIsClaimedCounts(float count, String value, boolean isClaimed) {
        this.count = count;
        this.value = value;
        this.isClaimed = isClaimed;
    }

    public static class CountAndBooleanRowMapper implements RowMapper<ValueAndIsClaimedCounts>
    {
        @Override
        public ValueAndIsClaimedCounts mapRow(ResultSet rs, int rowNum) throws SQLException
        {
            return new ValueAndIsClaimedCounts(
                    rs.getFloat(FIELD_COUNT),
                    rs.getString(FIELD_VALUE),
                    rs.getBoolean(FIELD_BOOL));
        }
    }
}
