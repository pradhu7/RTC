package com.apixio.dao.apxdata;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.protobuf.Message;

import com.apixio.util.BytesUtil;
import com.apixio.util.EncryptUtil;
import com.apixio.util.Pair;
import com.apixio.util.ZipUtil;

public class ChunkUtil<T extends Message>
{
    /**
     * Specifies how large the chunks of signal/prediction data are that are passed in
     * List<byte[]> to BlobStore.writeBlobs().  This value MUST be smaller than
     * BytesUtil.MAX_TOTAL_SIZE
     */
    private static final int MAX_DATA_CHUNK = 10 * (1024*1024);

    /**
     * Specifies how many items of data are allowed in a chunk.  This value must be 
     * less than BytesUtil.MAX_ELEMENT_COUNT
     */
    private static final int MAX_ELEMENT_COUNT = BytesUtil.MAX_ELEMENT_COUNT / 2;

    /**
     * Signal data is stored as byte[] and in order to support different formats within the raw
     * bytes the code tags things via the first byte in the persisted data.  These consts
     * define the format and how to pack/unpack things.  The actual converter used supplies the
     * format along with flags that indicate extra operations that SignalLogic code does (e.g.,
     * zipping and encryption).  The lowest 3 bits of the tag byte contains global options
     * (which are orthogonal to other options), while the upper 5 bits are the type/version
     * number.
     *
     * The design of tagging support includes the ability to deserialize older versions of
     * data; serialization is always done using the preferred converter.
     */
    final static int TAGDATA_FLAG_ZIPPED    = 0x1;     // store ZipUtil.zipBytes(packArray(converter.serialize()))
    final static int TAGDATA_FLAG_ENCRYPTED = 0x2;     // done after packing
    final static int TAGDATA_FLAG_RESERVED2 = 0x4;     // global option bit2

    /**
     * Does generic chunking of serialized/zipped data items into a caller-supplied Map<>
     * that is suitable for passing to BlobStore.writeBlobs().
     *
     * Because we could get lots (and lots and lots) of relatively small items (a few
     * hundred bytes) we need to group them into larger groups before zipping them in
     * order to make compression and Cassandra writes more efficient.
     *
     * The handling of zipped and non-zipped data is almost the same.  The tag byte is
     * always the first element in the returned List<byte[]> even though it's not that
     * relatively efficient in storage (but it makes this code much simpler).  All other
     * elements of the list are a packed array of the list of serialized data where the
     * total size of the serialized data is around the max chunk size, with the zipping
     * operation being done after the packing of arrays, as directed.
     */
    public Pair<Integer, List<byte[]>> serializeAndChunkData(EncryptInfo encryptInfo, DataType<T> converter, List<T> items) throws IOException
    {
        byte[]       tag         = makeTagByte(encryptInfo.encrypt, converter);
        boolean      zipit       = zipFlag(tag);
        List<byte[]> toPack      = new ArrayList<>(items.size() / 40);  // no array copying if each item serializes to > 40 bytes
        List<byte[]> chunks      = new ArrayList<>();
        int          total       = 0; // used only to end a chunk
        int          size        = 0; // total size of all chunks

        // the initialCapacity of toPack is set according to item size because for a very large item list
        // we want to avoid array copies of large arrays if we can.  ArrayList impls has growth factor
        // of 1.5 so if item serialization is much smaller than 40 we still only copy large arrays a few times

        // this method's return value is [total size of all chunks, list of byte arrays]
        // where the list of byte arrays is really just the chunked packing of the
        // concatenated byte arrays of the serialized items.  the first element of the
        // chunk list is a special case as it should always be a byte array of length 1,
        // where that single byte is just the tag byte that identifies the converter.
        //
        // chunks are at most MAX_DATA_CHUNK length (and likely quite a bit smaller) and
        // are formed by serializing items one by one and appending the byte arrays that
        // result from each serialization to packed array until the total size would be
        // greater than the max, then optionally zipping the packed array.
        //
        // sort of graphically, where "chunks" is the returned List<byte[]>:
        //
        //  chunks.get(0):  byte array of size 1, where byte[0] is converter tag byte
        //  chunks.get(1):  byte array of size < MAX; it is the value of packArrays() on
        //                    serialize(items.get(0)),
        //                    serialize(items.get(1)),
        //                    ...
        //                    serialize(items.get(n))
        //                  where the packArray() value is optionally zipped (where byte[] -> byte[])
        //    such that the sum of lengths of the serialize() results < MAX (prior to any zipping)
        //  chunks.get(2):  ...same as chunks.get(1) but for items.get(n+1), ...

        chunks.add(tag);    // inefficient to store a single byte but greatly simplifies code
        size += tag.length;

        for (T item : items)
        {
            byte[] ser = converter.serialize(item);

            if ((toPack.size() > 0) &&
                ((total + ser.length) > MAX_DATA_CHUNK) || (toPack.size() > MAX_ELEMENT_COUNT))
            {
                size += chunkData(encryptInfo, zipit, toPack, chunks);
                toPack.clear();
                total = 0;
            }

            total += ser.length;
            toPack.add(ser);
        }

        // deal with leftover
        if (toPack.size() > 0)
            size += chunkData(encryptInfo, zipit, toPack, chunks);

        return new Pair(size, chunks);
    }

    private static int chunkData(EncryptInfo encryptInfo, boolean zip, List<byte[]> data, List<byte[]> destList)
    {
        byte[] packed = BytesUtil.packArrays(data);
        double size   = packed.length;  // will never be 0

        if (zip)
            packed = ZipUtil.zipBytesSafe(packed);

        if (encryptInfo.encrypt)
            packed = EncryptUtil.encryptBytes(encryptInfo.pdsID, packed);

        destList.add(packed);

        //        System.out.println(" DEBUG chunkData compression=" + (((double) packed.length) / size));
        //        return ((double) packed.length) / size;

        return packed.length;
    }

    /**
     * Returns true iff tag info says to zip data
     */
    static boolean zipFlag(byte[] tag)
    {
        return (tag[0] & TAGDATA_FLAG_ZIPPED) != 0x0;
    }

    static boolean encryptFlag(byte[] tag)
    {
        return (tag[0] & TAGDATA_FLAG_ENCRYPTED) != 0x0;
    }

    private byte[] makeTagByte(boolean encrypt, DataType<T> converter)
    {
        return new byte[] {
            (byte) (converter.getTagInfo() | ((encrypt) ? TAGDATA_FLAG_ENCRYPTED : 0x0))
        };
    }

}
