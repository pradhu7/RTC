package com.apixio.dao.seqstore;

import com.apixio.dao.seqstore.utility.Range;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by dyee on 12/2/16.
 */
public class RangeTest {
    long startDate = 0;
    long endDate = Long.MAX_VALUE;


    @Test
    public void testIncludeRange() {
        Range range = new Range.RangeBuilder().setEnd(endDate)
                                            .setStart(startDate).setIncludeLower(true).setIncludeUpper(true).build();

        Assert.assertTrue(range.isIncludeLower());
        Assert.assertTrue(range.isIncludeUpper());
        Assert.assertEquals(startDate, range.getStart());
        Assert.assertEquals(endDate, range.getEnd());
    }

    @Test
    public void testExcludeRange() {
        Range range = new Range.RangeBuilder().setEnd(endDate)
                .setStart(startDate).setIncludeLower(false).setIncludeUpper(false).build();

        Assert.assertFalse(range.isIncludeLower());
        Assert.assertFalse(range.isIncludeUpper());
        Assert.assertEquals(startDate, range.getStart());
        Assert.assertEquals(endDate, range.getEnd());
    }

}
