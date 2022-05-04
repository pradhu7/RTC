package com.apixio.sdk.util;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.apixio.sdk.Converter;
import com.apixio.sdk.protos.FxProtos.FxTag;
import com.apixio.sdk.protos.FxProtos.FxType;
import com.apixio.sdk.protos.FxProtos.SequenceInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.util.Base64;
import com.google.protobuf.Message;
import com.google.protobuf.Parser;

public class TypeUtil
{

    static TypeReference<List<String>> stringListRef = new TypeReference<List<String>>() {};
    public static FxType stringType = FxIdlParser.parse("string fake()").getReturns();

    public static boolean isSequenceOfContainer(FxType type)
    {
        if (type.getTag() == FxTag.SEQUENCE)
        {
            SequenceInfo si = type.getSequenceInfo();

            type = si.getOfType();

            if (type.getTag() == FxTag.CONTAINER)
                return true;
        }

        return false;
    }

    public static List<Object> flattenListOfLists(List<Object> listOfSomething)
    {
        if ((listOfSomething.size() > 0) && (listOfSomething.get(0) instanceof List))
        {
            List<Object> flat = new ArrayList<>();

            for (Object e : listOfSomething)
                flat.addAll((List) e);

            return flat;
        }
        else
        {
            return listOfSomething;
        }
    }

    public static FxType protoStringToFxType(String b64string) throws Exception 
    {
        byte[] bytes = Base64.decodeBase64(b64string);
        return FxType.newBuilder().build().getParserForType().parseFrom(bytes);
    }

    public static Object protoStringToInterface(Converter converter, String b64string) throws Exception 
    {
        byte[] bytes = Base64.decodeBase64(b64string);
        Class<? extends Message> protoclass = converter.getMetadata().protoClass;
        Method buildermethod = protoclass.getMethod("newBuilder");
        Object builder = buildermethod.invoke(null);
        Method buildmethod = builder.getClass().getMethod("build");
        Message message = (Message) buildmethod.invoke(builder);
        Parser<? extends Message> parser = message.getParserForType();
        return converter.convertToInterface(parser.parseFrom(bytes));
    }

    public static String interfaceToProtoString(Converter converter, Object arg)
    {
        Message proto = converter.convertToProtobuf(arg);
        return protoToBase64String(proto);
    }

    public static String protoToBase64String(Message proto)
    {
        return Base64.encodeBase64String(proto.toByteArray());
    }

    public static List<String> jsonToStringList(String json) throws Exception {
        return new ObjectMapper().readValue(json, stringListRef);
    }

	public static String mapToJsonString(Map<String, Object> payload) throws JsonProcessingException {
		return new ObjectMapper().writeValueAsString(payload);
	}


    public static List<Object> convertJsonListToInterfaces(Converter outconverter, String jsonList) throws Exception {
        List<String> rawresults = jsonToStringList(jsonList);
        return convertStringListToInterfaces(outconverter, rawresults);
    }
    public static List<Object> convertStringListToInterfaces(Converter outconverter, List<String> rawresults) throws Exception {

        List<Object> results = rawresults.stream().map(k -> {
            try {
                return protoStringToInterface(outconverter, k);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }).collect(Collectors.toList());
        return results;
    }
    

}
