package com.apixio.model.assembly;

import java.util.List;

/**
 * Assembler is the thing that operates ON the assembly things.  The class param "A"
 * is the type of the assembly (e.g., Patient), and "P" is the type of the part
 * (which, for patient stuff is also Patient).
 *
 * There is one implementation of Assembler for each category of an Assembly.
 */
public interface Assembler<A,P>
{
    /**
     * "Scope" of a merge declares what things in an assembly are to be merged.
     * The infrastructure divides these things into two buckets currently:
     *
     *  * parts:  extracted parts with a non-null PartID
     *  * aggregate:  the single chunk that aggregates all parts; within code it
     *    is when partID==null
     *
     * The enums should be self-descriptive...
     */
    public enum MergeScope
    {
        PARTS_ONLY,
        AGGREGATE_ONLY,
        PARTS_AND_AGGREGATE
    }

    /**
     * Information about a merge operation that could be useful to merge code
     */
    public static class MergeInfo
    {
        private MergeScope mergeScope;
        private boolean    inMemory;

        public MergeInfo(MergeScope mergeScope, boolean inMemory)
        {
            this.mergeScope = mergeScope;
            this.inMemory   = inMemory;
        }

        public MergeScope getMergeScope()
        {
            return mergeScope;
        }

        public boolean isInMemoryMerge()
        {
            return inMemory;
        }
    }

    /**
     * Wrapper for Parts so we can pass in per-part meta/extra.
     */
    public static class PartEnvelope<P>
    {
        public P    part;
        public long ts;

        public PartEnvelope(P part, long ts)
        {
            this.part = part;
            this.ts   = ts;
        }
    }

    /**
     * Convert wrapped object to byte[] in preparation for storage.
     */
    // public byte[] serialize(P p, String scope);
    public byte[] serialize(P p, String scope) throws Exception;

    /**
     * Inverse of serialize
     */
    public P deserialize(byte[] bytes);

    /**
     * Extracts the given category part(s) from the instance.
     */
    public List<Part<P>> separate(A instance);

    /**
     * Merges the list of Parts into a single Part
     */
    public PartEnvelope<P> merge(MergeInfo info, PartEnvelope<P> p1, PartEnvelope<P> p2);
}
