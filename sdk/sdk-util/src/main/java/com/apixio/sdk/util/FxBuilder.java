package com.apixio.sdk.util;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.protobuf.util.JsonFormat;

import com.apixio.sdk.protos.FxProtos.ContainerInfo;
import com.apixio.sdk.protos.FxProtos.EnumInfo;
import com.apixio.sdk.protos.FxProtos.FxDef;
import com.apixio.sdk.protos.FxProtos.FxImpl;
import com.apixio.sdk.protos.FxProtos.FxTag;
import com.apixio.sdk.protos.FxProtos.FxType;
import com.apixio.sdk.protos.FxProtos.MapInfo;
import com.apixio.sdk.protos.FxProtos.SequenceInfo;

/**
 * Utility class to help build protobuf POJOs for (manually "parsed") FxDef and FxImpl representations that
 * can be persisted and restored.
 *
 * Use of this class can be confusing as it requires that the client code pretend it's parsing some f(x)
 * definition, building up the elements of it along the way.  It requires that the Class<?> objects for
 * the types be available in the JVM that's being used to build this persistable representation.
 */
public class FxBuilder
{
    private JsonFormat.Printer formatter = JsonFormat.printer(); //.omittingInsignificantWhitespace();

    /**
     *
     */
    public static FxDef makeFxDef(String name, FxType returns, List<FxType> parameters)
    {
        FxDef.Builder defBuilder = FxDef.newBuilder();

        defBuilder.setName(name);
        defBuilder.setReturns(returns);

        for (FxType param : parameters)
            defBuilder.addParameters(param);

        return defBuilder.build();
    }

    /**
     *
     */
    public static FxImpl makeFxImpl(FxDef def, URL implUrl, String entryName, Map<String,String> assets)
    {
        FxImpl.Builder builder = FxImpl.newBuilder();

        builder.setFxDef(def);
        builder.setEntryName(entryName);

        if (implUrl != null)
            builder.setImplUrl(implUrl.toString());

        if (assets != null)
        {
            for (Map.Entry<String,String> entry : assets.entrySet())
                builder.putAssets(entry.getKey(), entry.getValue());
        }

        return builder.build();
    }

    /**
     * Scalar factory
     */
    public static FxType makeIntType()
    {
        return makeScalarType(FxTag.INT);
    }

    public static FxType makeLongType()
    {
        return makeScalarType(FxTag.LONG);
    }

    public static FxType makeFloatType()
    {
        return makeScalarType(FxTag.FLOAT);
    }

    public static FxType makeDoubleType()
    {
        return makeScalarType(FxTag.DOUBLE);
    }

    public static FxType makeStringType()
    {
        return makeScalarType(FxTag.STRING);
    }

    public static FxType makeBooleanType()
    {
        return makeScalarType(FxTag.BOOLEAN);
    }

    /**
     * Non-scalars
     */
    public static FxType makeEnumType(Class<? extends Enum> eclz, String... ids)
    {
        // fxtype of ENUM
        FxType.Builder   fxBuilder = FxType.newBuilder();
        EnumInfo.Builder enBuilder = EnumInfo.newBuilder();

        enBuilder.setName(eclz.getCanonicalName());
        for (String id : ids)
            enBuilder.addIdentifiers(id);

        fxBuilder.setTag(FxTag.ENUM);
        fxBuilder.setEnumInfo(enBuilder.build());

        return fxBuilder.build();
    }

    public static FxType makeStructType(Class<?> clz, Map<String, FxType> fields)
    {
        // fxtype of struct (as container)
        return makeContainerType(clz, true, fields);
    }

    public static FxType makeRefStructType(String clzName)
    {
        // fxtype of struct (as container)
        return makeContainerType(clzName, true, null);
    }

    public static FxType makeUnionType(Class<?> clz, Map<String, FxType> fields)
    {
        // fxtype of union (as container)
        return makeContainerType(clz, false, fields);
    }

    public static FxType makeContainerType(Class<?> clz, boolean isStruct, Map<String, FxType> fields)
    {
        // fxtype of union (as container)

        return makeContainerType(clz.getCanonicalName(), isStruct, fields);
    }

    private static FxType makeContainerType(String clzName, boolean isStruct, Map<String, FxType> fields)
    {
        // fxtype of union (as container)
        FxType.Builder        fxBuilder = FxType.newBuilder();
        ContainerInfo.Builder cnBuilder = ContainerInfo.newBuilder();

        cnBuilder.setName(clzName);
        cnBuilder.setIsStruct(isStruct);

        if (fields != null)
        {
            for (Map.Entry<String,FxType> entry : fields.entrySet())
                cnBuilder.putFields(entry.getKey(), entry.getValue());
        }

        fxBuilder.setTag(FxTag.CONTAINER);
        fxBuilder.setContainerInfo(cnBuilder.build());

        return fxBuilder.build();
    }

    public static FxType makeSequenceType(FxType ofType)
    {
        // fxtype of list
        FxType.Builder       fxBuilder = FxType.newBuilder();
        SequenceInfo.Builder sqBuilder = SequenceInfo.newBuilder();

        sqBuilder.setOfType(ofType);

        fxBuilder.setTag(FxTag.SEQUENCE);
        fxBuilder.setSequenceInfo(sqBuilder.build());

        return fxBuilder.build();
    }

    /**
     * Implementation
     */
    private static FxType makeScalarType(FxTag stype)
    {
        return (FxType.newBuilder().setTag(stype)).build();
    }

}
