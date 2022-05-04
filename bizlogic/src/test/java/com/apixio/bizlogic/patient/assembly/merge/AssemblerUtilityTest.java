package com.apixio.bizlogic.patient.assembly.merge;

import com.apixio.bizlogic.patient.assembly.utility.AssemblerUtility;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Assert;
import org.junit.Test;

public class AssemblerUtilityTest {

    public final static int TEST_SIZE = 8000000; // 8 million bytes

    @Test
    public void benchmarkStringFormatHexConversion() {
        final byte[] randomBytes = RandomUtils.nextBytes(TEST_SIZE);

        long t0 = System.currentTimeMillis();

        final String hexRepr1 = AssemblerUtility.byteArray2Hex(randomBytes);

        long t1 = System.currentTimeMillis();

        float sec1 = (t1 - t0) / 1000.0f;

        System.out.println("byteArray2Hex time : " + sec1 + " seconds");

        long t2 = System.currentTimeMillis();

        final String hexRepr2 = Hex.encodeHexString(randomBytes);

        long t3 = System.currentTimeMillis();

        // It is very likely that if the two outputs are identical for 8 million random bytes
        // that the functions are equivalent
        Assert.assertEquals(hexRepr1, hexRepr2);

        float sec2 = (t3 - t2) / 1000.0f;

        System.out.println("Hex.encodeHexString time : " + sec2 + " seconds");

        // Hex implementation should be at least twice as fast as string implementation
        Assert.assertTrue(sec2 < (sec1 / 2));
    }
}
