package com.apixio.sdk;

import com.google.protobuf.Message;

/**
 * Converter classes transform the interface-based representation of data used directly
 * by f(x) implementations to a persistable form of the data (with actual
 * (de)serialization performed by protobuf generated classes)

 * The "ID" attribute inherited from Plugin class defines the prefix of the storage key
 * and the classname of the protoClass is used for logging/debugging.  These two can
 * be made the same if desired.
 */
public interface Converter<I,P extends Message> extends Plugin
{
    /**
     * All info needed to create instances of ApxDataDao's DataType.  Encrypt flag belongs here
     * as the "has PHI" or "doesn't have PHI" is intrinsic to the data and not to a particular
     * application.
     *
     * If convertToProtobuf is called with a Class<?> that's different from protoClass, then
     * an exception is thrown; interfaceClass is handled the same way.
     */
    public class Meta
    {
        public Class<?>                 interfaceClass;
        public Class<? extends Message> protoClass;
        public boolean                  encryptFlag;

        public Meta(Class<?> ifcClass, Class<? extends Message> protoClass, boolean encryptFlag)
        {
            this.interfaceClass = ifcClass;
            this.protoClass     = protoClass;
            this.encryptFlag    = encryptFlag;
        }
    }

    /**
     * Return info used to build a DataType that's usable by ApxDataDao
     */
    public Meta getMetadata();

    /**
     * Convert from interface to protobuf.
     */
    public P convertToProtobuf(I obj);

    /**
     * Convert from protobuf to interface.
     */
    public I convertToInterface(P obj);

}
