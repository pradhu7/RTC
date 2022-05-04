package com.apixio.sdk;

import java.net.URI;
import java.util.List;

import com.apixio.dao.apxdata.ApxDataDao.QueryKeys;
import com.apixio.sdk.protos.EvalProtos.ArgList;

/**
 * A DataUriManager is responsible for the construction and deconstruction of dataURIs and
 * related elements.  A DataURI is a well-formed URI (not URL) that identifies some group
 * of data as returned from an f(x) implementation and peristed via the SDK system.  It
 * is meant to be a standardized way of referring to persisted output of f(x) that should
 * be used by system components (e.g., cerebro, applications, etc).
 *
 * Note that it's expected/desired that the schemes supported for a data URI, and the
 * capabilities of those schemes, expand over time as the sources/storage mechanisms
 * and features for storing and organizing data improve (e.g., when a real feature
 * store becomes operational).
 *
 * When writing the output of some f(x) invocation the following metadata are required to
 * be produced:
 *
 *  1) QueryKeys (as defined in ApxDataDao area)
 *  2) groupingID (ditto)
 *  3) a DataURI, to be returned to requestor of f(x) invocation; the protoclass msut be part of this
 *
 * Additionally, on reading that persisted output, we have to convert from a DataURI to:
 *
 *  1) the protoclass
 *  2) QueryKeys
 *  3) groupingID, which can come from using QueryKeys
 *
 * An implementation of DataUriManager is responsible for the above operations, in conjunction
 * with accessors, as necessary.
 *
 * A DataUriManager usually (always?) needs contextual information, likely from the request; e.g.
 * a docUUID and function identifier are often needed as part of the groupingID.  The mechanism
 * to support this is the "evalArgs" idea of the generic ECC:  a csv of expressions, one for each
 * required bit of data from the context.  The expressions are evaluated when required to get
 * the info.
 */
public interface DataUriManager extends FxComponent
{

    /**
     * These args are evaluated as needed to produce query keys, etc.  These can be set only
     * once for each object instance.
     */
    public void setPrimaryKeyArgs(ArgList args, List<String> allowedKeys) throws Exception;

    /**
     * This is a client-optional operation that provides a mechanism to create a unique
     * partition ID when saving f(x) output for multiple invocations for a given primary key
     * where the output is to be appended/collected rather than replaced.
     */
    public void setPartitionArgs(ArgList args) throws Exception;

    /**
     * Produces the QueryKeys that are to be added when saving data.  An implicit "and"
     * operation must be done when querying with the same query keys on reading.  Null
     * is a valid return and indicates there's no need for query keys.
     */
    public QueryKeys makeQueryKeys(FxRequest req) throws Exception;

    /**
     * Produces the low-level unique storage ID for this dataset.  A null return value is
     * not allowed.
     */
    public String makeGroupingID(FxRequest req) throws Exception;

    /**
     * This is a client-optional operation in that the usual case is to return null or "" (both
     * of which are treated identically).  If a non-null value is returned, it's derived from
     * the ArgList passed to setPartitionArgs() and is used to create the partitionID used when
     * persisting f(x) output data.  A non-null value should be returned if it's expected that
     * append semantics are required for the output of multiple invocations of an f(x) for the
     * same primary key.
     */
    public String makePartitionID(FxRequest req) throws Exception;

    /**
     * Create the URI that can be used to later retrieve the data
     */
    public URI makeDataURI(FxRequest req) throws Exception;

    /**
     * Deconstruct the URI and return its possibly underlying groupingID used for actual storage.
     * This can return null as long as getQueryKeys() returns non-null.
     */
    public String getGroupingID(URI dataURI) throws Exception;

    /**
     * Deconstruct the URI and return its QueryKeys that can be used to get the groupingID
     */
    public QueryKeys getQueryKeys(URI dataURI) throws Exception;

}
