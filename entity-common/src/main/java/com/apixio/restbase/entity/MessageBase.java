package com.apixio.restbase.entity;

import java.lang.reflect.Constructor;
import java.util.Date;

import com.apixio.XUUID;
import com.apixio.utility.StringUtil;

/**
 * A non-entity type of persistence (meaning that the items added to a MessageList
 * are not uniquely identifiable via an XUUID).
 *
 * Message serialization/deserialization is a bit different from the BaseEntity model
 * (due to the lack of entity-ness like XUUID).  Serialization uses the normal ParamSet
 * but the classname of the instance is included as a field (with the name "_classname").
 * Deserialization reconstructs the ParamSet and then does a Class.forName.newInstance
 * with the ParamSet.
 *
 * All classes that extend MessageBase MUST have a public constructor that accepts an
 * instance of ParamSet; this constructor must call "super(fields);".  This model is in
 * contrast to the entity-based reconstruction.
 */
public class MessageBase extends PersistBase
{
    /**
     * Names of persisted fields.  Any change in names will make persisted items unrestorable.
     */
    protected final static String F_INSTANCECLASSNAME = "_classname";

    /**
     * actual fields
     */

    /**
     * Create a new Message
     */
    protected MessageBase()
    {
        super();
    }

    /**
     * For restoring from persisted form only
     */
    protected MessageBase(ParamSet fields)
    {
        super(fields);
    }

    /**
     *
     */
    final public String serialize()
    {
        return StringUtil.mapToString(produceFieldMap().toMap());
    }

    /**
     * Reconstructs the POJO given the output of serialize().  This reconstruction uses
     * reflection to create tne properly-typed object as there is not intended to be
     * multiple types of DAOs that reconstruct the elements of the MessageList.
     */
    final public static MessageBase deserialize(String ser)
    {
        ParamSet fields = new ParamSet(StringUtil.mapFromString(ser));
        String   clazz  = fields.get(F_INSTANCECLASSNAME);

        if (clazz != null)
        {
            try
            {
                Class<MessageBase>       clz  = (Class<MessageBase>) Class.forName(clazz);
                Constructor<MessageBase> ctor = clz.getConstructor(ParamSet.class);

                return ctor.newInstance(fields);
            }
            catch (Exception x)
            {
                // ought to log this better
                x.printStackTrace();
            }
        }

        return null;
    }

    /**
     * Getters
     */

    /**
     * Setters
     */

    /**
     * Return a Map of field=value for persisting the object.
     */
    protected void toParamSet(ParamSet fields)
    {
        super.toParamSet(fields);

        fields.put(F_INSTANCECLASSNAME, this.getClass().getCanonicalName());
    }

}
