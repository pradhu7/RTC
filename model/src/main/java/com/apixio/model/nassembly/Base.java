package com.apixio.model.nassembly;

import java.io.Serializable;

/**
 * The base interface of all assembly classes
 */

// All the system defined columns start with x_

public interface Base extends Serializable
{
    /**
     * The assembly framework guarantees that every table has a column called "cid".
     *
     * Note:
     * - We hardcode the name of the column. We don't want to give freedom to any developer
     * to modify it.
     * - We have to make sure that it doesn't conflict with any protobuf fields!!!
     */
    String Cid = "x_cid";
    String Oid = "x_oid";

    /**
     * Some tables are linked to other tables (like foreign key relationship)
     *
     * Note:
     * - We hardcode the name of the column. We don't want to give freedom to any developer
     * to modify it.
     * - We have to make sure that it doesn't conflict with any prtobuf fields!!!
     */
    String FromCid = "x_fromCid";

    /**
     * This is the default domain in the system
     */
    String defaultDomainName = "patient";

    /**
     * Returns the name of the datatype
     *
     * @param
     * @return
     */
    String getDataTypeName();

    /**
     * Returns true if it matches the correct assembly implementation
     *
     * @param dataTypeName
     * @return
     *
     * Note: There is no reason to overwrite this method (java doesn't have final method for interfaces)
     */
    default boolean matchClass(String dataTypeName){ return getDataTypeName().equalsIgnoreCase(dataTypeName); }
}
