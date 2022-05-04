package com.apixio.util;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Utility functions on byte arrays to help in persistence of signal data
 */
public class BytesUtil
{
    /**
     * First value of packed byte[]; written via ByteBuffer.putInt(PACKED_VERSION).
     */
    public static final int PACKED_VERSION = 1;

    /**
     * Limits.  Total size is limited because we need that much contiguous heap
     */
    public static final int MAX_ELEMENT_COUNT = 65536;               // arbitrary
    public static final int MAX_TOTAL_SIZE    = 16 * (1024 * 1024);  // 16M
    public static final int MIN_TOTAL_SIZE    = Integer.BYTES * 3;   // ver, tot, #elements

    /**
     * Combines the (ordered) list of byte arrays into a single byte array with
     * meta info prefixed to allow reconstruction of the original list of byte
     * arrays via unpackArrays().
     *
     * This is done by creating a byte[] that contains:
     *
     *  version number; for possible format changes; recorded via ByteBuffer.putInt(1)
     *  total size; for checking validity; via ByteBuffer.putInt(sz); includes ALL bytes
     *  # of elements in arrays; via ByteBuffer.putInt()
     *  size of list.get(0), via ByteBuffer.putInt(list.get(0).length)
     *  ...
     *
     * then all byte arrays are appended via ByteBuffer.put(ByteBuffer)
     */
    public static byte[] packArrays(List<byte[]> arrays)
    {
        ByteBuffer packed;
        int        totalSize;

        if (arrays.size() > MAX_ELEMENT_COUNT)
            throw new IllegalArgumentException("Can't pack more than " + MAX_ELEMENT_COUNT + " byte arrays; got " + arrays.size());

        totalSize = (Integer.BYTES +                  // version
                     Integer.BYTES +                  // total size
                     Integer.BYTES +                  // # of elements
                     arrays.size() * Integer.BYTES    // sizes of each element
            );

        for (byte[] bs : arrays)
            totalSize += bs.length;

        if (totalSize > MAX_TOTAL_SIZE)
            throw new IllegalArgumentException("Total size of packed byte[] (" + totalSize + ") exceeds max (" + MAX_TOTAL_SIZE + ")");

        packed = ByteBuffer.allocate(totalSize);

        packed.putInt(PACKED_VERSION);
        packed.putInt(totalSize);

        packed.putInt(arrays.size());

        for (byte[] bs : arrays)
            packed.putInt(bs.length);

        for (byte[] bs : arrays)
            packed.put(ByteBuffer.wrap(bs));

        return packed.array();
     }

    private static int checkVersion(int ver)
    {
        if (ver != PACKED_VERSION)
            throw new IllegalStateException("Expected version of packed byte[] to be " + PACKED_VERSION +
                                            " but got " + ver);

        return ver;
    }

    private static int checkTotal(int total)
    {
        if (total < MIN_TOTAL_SIZE)
            throw new IllegalStateException("Expected internal total size of packed byte[] to be at least " + MIN_TOTAL_SIZE + " but was " + total);
        else if (total > MAX_TOTAL_SIZE)
            throw new IllegalStateException("Total size of packed byte[] (" + total + ") exceeds max (" + MAX_TOTAL_SIZE + ")");

        return total;
    }

    private static int checkCount(int count)
    {
        if (count < 0)
            throw new IllegalStateException("Expected internal count of element of packed byte[] to be non-negative but was " + count);
        else if (count > MAX_ELEMENT_COUNT)
            throw new IllegalStateException("Unpack of packed byte[]:  number of elements (" + count + ") more than allowed (" + MAX_ELEMENT_COUNT + ")");

        return count;
    }

    private static void checkSizes(int[] sizes, int totalSize)
    {
        int total = Integer.BYTES * sizes.length;

        for (int sz : sizes)
            total += sz;

        // calculated total should be less than declared totalSize (and should be
        // different by MIN_TOTAL_SIZE...)

        if (total >= totalSize)
            throw new IllegalStateException("Sum of element sizes ( " + total + ") larger that expected (" + totalSize + ")");
    }

    /**
     * Reverses what packByteArrays does.
     */
    public static List<byte[]> unpackArrays(byte[] packedArrays)
    {
        ByteBuffer   packed = ByteBuffer.wrap(packedArrays);
        int          ver    = checkVersion(packed.getInt());
        int          total  = checkTotal(packed.getInt());
        int          count  = checkCount(packed.getInt());
        int[]        sizes  = new int[count];
        List<byte[]> arrays;

        for (int i = 0; i < count; i++)
            sizes[i] = packed.getInt();

        checkSizes(sizes, total);

        arrays = new ArrayList<>(count);

        for (int i = 0; i < count; i++)
        {
            byte[] bs = new byte[sizes[i]];

            packed.get(bs);
            arrays.add(bs);
        }

        return arrays;
    }

    /**
     * Test only
     */
    private static void dump(byte[] bs)
    {
        Base64.Encoder en = Base64.getEncoder();

        System.out.println(en.encodeToString(bs));
    }

    public static void main(String... args) throws Exception
    {
        List<byte[]> strBytes = new ArrayList<>();

        for (String a : args)
            strBytes.add(a.getBytes("UTF-8"));

        byte[]       packed   = packArrays(strBytes);
        List<byte[]> unpacked = unpackArrays(packed);

        for (byte[] ba : unpacked)
            System.out.println(new String(ba));
    }

}

    
