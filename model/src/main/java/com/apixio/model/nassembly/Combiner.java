package com.apixio.model.nassembly;

import java.util.*;

/**
 * The interface to combine together heterogeneous or homogeneous assembly objects
 * Combine should be able to combine any number of derived objects as long as they
 * have a join key (the combinedId)
 *
 * @param <T>
 */
public interface Combiner<T extends Exchange> extends Base
{


    class CombinerInput {
        private Map<String, ? extends Exchange[]> dataTypeNameToExchanges;
        private Map<String, Object> passThruData; // metadata / data from accessors, etc


        public CombinerInput(Map<String, ? extends Exchange[]> dataTypeNameToExchanges, Map<String, Object> passThruData) {
            this.dataTypeNameToExchanges = dataTypeNameToExchanges;
            this.passThruData = passThruData;
        }

        /**
         * Used for tracking/logging purposes only
         *
         * @return the oid (such as archive uuid)
         */
        public Map<String, ? extends Exchange[]> getDataTypeNameToExchanges()
        {

            return this.dataTypeNameToExchanges;
        }

        public Map<String, Object> getPassThruData() {
            return passThruData;
        }
    }


    /**
     * Get all the data type names for building combined types
     *
     * @return
     */
    Map<String, Aggregator> fromDataTypeToAggregator();

    default AggregatorJoinStrategy getJoinStrategy(){return new GroupIdUnionAggregatorJoinStrategy();}

    // Helper function to just return input data type names
    default Set<String> getInputDataTypeNames() {return fromDataTypeToAggregator().keySet();}

    /**
     * Given a map of exchanges, return an iterable of new ones of different type or of the same type
     *
     * @param input to the combiner
     * @return
     */
    Iterable<T> combine(CombinerInput input);


    /**
     * Allow us to describe optimal data sorting
     * @return
     */
    default SortedFieldOrder[] getSortedFields() {return new SortedFieldOrder[]{} ;}

    /**
     * @return true if it queryable and needs to be persisted to data lake
     */
    default boolean isQueryable(){ return  true; }


    default List<Accessor<?>> getAccessors(){return Collections.emptyList();};
}
