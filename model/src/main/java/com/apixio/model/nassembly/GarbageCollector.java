package com.apixio.model.nassembly;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The interface to merge two homogeneous assembly objects.
 *
 */
public interface GarbageCollector extends Base {


    /**
     * Get the field names (column names) to group similar objects together
     *
     * @return
     */
    default Set<String> getGroupByFieldNames() {return Stream.of(Cid).collect(Collectors.toSet());}

    /**
     * Describes the field names (column names) with the sorting order that
     * objects are sorted on
     *
     * Notes:
     * - Used only for build in merge
     * - Order matters
     *
     * @return
     */
    default SortedFieldOrder[] getSortedFields() {throw new UnsupportedOperationException("Not Yet Implemented");}


    /**
     * The condition used to filter merged rows
     *
     * @return
     */
    default TransformationMeta[] getPostProcesses() {
        return new TransformationMeta[]{};
    }

    /**
     * It returns the name of sources for an exchange. It general, they are the same datatype as the merged datatype
     *
     * @return
     */
    default Set<String> getInputDataTypeNames() {return Stream.of(getDataTypeName()).collect(Collectors.toSet());}

    /**
     * @return true if it queryable and needs to be persisted to data lake
     */
    default boolean isQueryable(){ return  true; }
}
