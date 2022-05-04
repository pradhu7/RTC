package com.apixio.model.nassembly;

import java.io.InputStream;
import java.util.Map;
import java.util.UUID;

import com.apixio.model.file.ApxPackagedStream;
import com.apixio.model.patient.Patient;
import com.google.protobuf.Descriptors.Descriptor;

/**
 * Exchange is a container for transforming, serializing, and de-serializing assembly objects.
 *
 */

public interface Exchange extends Base
{
    class ProtoEnvelop
    {
        private String oid; // Used for tracking/logging purposes only (such as archive uuid)
        private String oidKey; // like the document key
        private byte[] protoBytes;
        private String protoJson;

        public ProtoEnvelop(String oid, String oidKey, byte[] protoBytes, String protoJson)
        {
            this.oid        = oid;
            this.oidKey     = oidKey;
            this.protoBytes = protoBytes;
            this.protoJson  = protoJson;
        }

        /**
         * Used for tracking/logging purposes only
         *
         * @return the oid (such as archive uuid)
         */
        public String getOid()
        {
            return oid;
        }

        /**
         * Get the oidKey (such as document key)
         *
         * @return
         */
        public String getOidKey()
        {
            return oidKey;
        }

        /**
         * Serialize data exchange into a protobuf byte array
         *
         * @return
         */
        public byte[] getProtoBytes()
        {
            return protoBytes;
        }

        /**
         * Get a proto json string representation of data exchange
         *
         * @return
         */
        public String getProtoJson()
        {
            return protoJson;
        }
    }

    /**
     * Each exchange belongs to domains
     *
     * Domains are important concept to the system as they decide how exchanges are linked together
     * For instance, exchanges that belong to the same domain are not linked together
     *
     *
     * @return
     */
    default String getDomainName() {return defaultDomainName; } // examples: patient or population or something

    /**
     * Get the Descriptor of the proto
     * @return
     */
    Descriptor getDescriptor();

    /**
     * Get the class id (group id) of the assembly exchange
     *
     * @return the class id (such as patient id)
     */
    String getCid();

    /**
     * Set the Cid and Primary External Id
     *
     * @param cid
     * @param primaryEid
     */
    default void setIds(UUID cid, byte[] primaryEid){throw new UnsupportedOperationException("Not yet Implemented");} // Throw exception so we dont have silent failrues

    /**
     * Get the primary eid
     */
    default String getPrimaryEid(){return "";}


    /**
     * Get all the external Ids for the cid
     * @return List of strings
     */
    default byte[][] getExternalIds(){throw new UnsupportedOperationException("Not yet Implemented");}

    /**
     * Get the eid Exchange
     */
    default Exchange getEidExchange(){return null;}

    /**
     * This is a common ppe-work before parsing
     *
     * @param ac
     */
    default Map<String,Object> preProcess(AssemblyContext ac){throw new UnsupportedOperationException("Not yet Implemented");}

    /**
     * Given an input, convert it to an internal representation of assembly exchange (i.e., one or more protobufs)
     *
     * @param fromNameToValue input data
     * @param ac  everything you need about the context of the evaluation
     */
    default void parse(Map<String,Object> fromNameToValue, AssemblyContext ac){throw new UnsupportedOperationException("Not yet Implemented");}

    /**
     * Given an apo, convert it to an internal representation of assembly exchange(i,e, one or more protobufs)
     *
     * @param apo
     * @param ac  everything you need about the context of the evaluation
     */
    default void parse(Patient apo, AssemblyContext ac){throw new UnsupportedOperationException("Not yet Implemented");}


    /**
     * Given a document inputstream and catalogEntry, convert it to an internal representation of assembly exchange (i.e., one or more protobufs)
     * @param inputFile CCDA, AXM, APX, etc apx packaged stream
     * @param sourceFileName name of input file
     * @param customerProperties info related to customer
     * @param ac Assembly content
     */
    default void parse(ApxPackagedStream inputFile, String sourceFileName, Object customerProperties, AssemblyContext ac){throw new UnsupportedOperationException("Not yet Implemented");}

    /**
     * Convert a top level exchange to the lower level parts
     * @param ac AseemblyContext
     */
    default Map<String,? extends Iterable<Exchange>> getParts(AssemblyContext ac){throw new UnsupportedOperationException("Not yet Implemented");}

    /**
     * Deserialize to data exchange internal representation given a protobuf byte array
     *
     * @param protoBytes
     */
    void fromProto(Iterable<byte[]> protoBytes);

    /**
     * Deserialize to data exchange internal representation given a protobuf inputStream
     *
     * @param inputStreams
     */
    void fromProtoStream(Iterable<InputStream> inputStreams);

    /**
     * Array of ProtoEnvelops
     *
     * @return
     */
    Iterable<ProtoEnvelop> getProtoEnvelops();

    /**
     * This method is for backward compatibility. It allows to convert an exchange (a protobuf) to an APO
     * so that all existing code works with the Data2020 framework
     *
     * @return
     */
    default Iterable<Patient> toApo(){throw new UnsupportedOperationException("Not yet Implemented");}
}