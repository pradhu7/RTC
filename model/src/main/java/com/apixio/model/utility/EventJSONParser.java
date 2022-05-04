package com.apixio.model.utility;

import java.io.IOException;

import org.joda.time.DateTime;

import com.apixio.model.EitherStringOrNumber;
import com.apixio.model.patient.event.Event;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

@Deprecated
public class EventJSONParser {
	
	private static ObjectMapper objectMapper = new ObjectMapper();
	
	static 
	{
		SimpleModule module1 = new SimpleModule("DateTimeDeserializerModule");
        module1.addDeserializer(DateTime.class, new ApixioDateDeserialzer());
        
        objectMapper.registerModule(module1);
        
        SimpleModule module2 = new SimpleModule("EitherStringOrNumberDeserializerModule");
        module2.addDeserializer(EitherStringOrNumber.class, new EitherStringOrNumberDeserializer());
        
        objectMapper.registerModule(module2);

        SimpleModule module3 = new SimpleModule("DateTimeSerializerModule");
        module3.addSerializer(DateTime.class, new ApixioDateSerializer());
        
        objectMapper.registerModule(module3);
        
        SimpleModule module4 = new SimpleModule("EitherStringOrNumberSerializerModule");
        module4.addSerializer(EitherStringOrNumber.class, new EitherStringOrNumberSerializer());
        
        objectMapper.registerModule(module4);
	}
	
	public static String toJSON(Event e) throws JsonProcessingException 
	{
		return objectMapper.writeValueAsString(e);
	}
	
	/**
	 * Given the input json, convert it to an event.
	 * @param inputJson
	 * @return an event object parsed from the input json.
	 * @throws IOException 
	 * @throws JsonMappingException 
	 * @throws JsonParseException 
	 */
	public static Event parseEvent(String inputJson) throws JsonParseException, JsonMappingException, IOException 
	{
		Event e = objectMapper.readValue(inputJson, Event.class);
		return e;
	}
	
	/**
	 * Given an event, converts it into a delimited text string, that can be used in
	 * various serialization and de-serialization scenarios. The order of fields here
	 * follow the order specified in the event stream definition document
	 * 	https://docs.google.com/a/apixio.com/document/d/1KzQC1gXVcnIyfiWEsYP8-AEqlVU5PUsT4ewqXPNbUao/edit?usp=sharing 
	 * as of 2/1/2013
	 * 
	 * @param e	the event to be serialized
	 * @param prefix the prefix that is to be added when serialized.
	 * @param suffix the suffix that is to be added when serialized.
	 * @param delim	the delimiter to be used, if null is specified, \t is automatically used as the default.
	 * @return	a delimited version of the event.
	 * @throws JsonProcessingException 
	 */
	public static String toDelimitedText(Event e, String prefix, String suffix, String delim) throws JsonProcessingException 
	{
		StringBuilder sb = new StringBuilder();
		sb.append((prefix==null?"":prefix));
		
		String delimClean = (delim==null?"\t":delim);
		// now we have to attach each attribute of the event as mentioned in the spec.
		
		// patientUUID ($1)
		sb.append(e.getPatientUUID().toString());
		sb.append(delimClean);
		
		// token or code name ($2)
		sb.append(e.getCode().getDisplayName());
		sb.append(delimClean);
		
		// code in the format codesystem:code ($3)
		sb.append(e.getCode().getCodingSystem()+":"+e.getCode().getCode());
		sb.append(delimClean);
		
		// event start date ($4)
		sb.append(e.getStartDate().toString());
		sb.append(delimClean);
		
		// event end date ($5)
		sb.append(e.getEndDate().toString());
		sb.append(delimClean);
		
		// encounter UUID ($6) - since this can be null, we have to make a null check before
		// writing it out.
		sb.append((e.getSourceEncounter()!=null?e.getSourceEncounter().toString():""));
		sb.append(delimClean);
		
		// objectUUID ($7)
		sb.append(e.getObjectUUID()==null?"":e.getObjectUUID().toString());
		sb.append(delimClean);
		
		// objectType ($8)
		sb.append(e.getObjectType()==null?"":e.getObjectType());
		sb.append(delimClean);
		
		// now the attribute value map will be serialized into a json object so that it
		// can be easily retrieved and stored  ($9)
		sb.append(objectMapper.writeValueAsString(e.getMetadata()));
		sb.append(delimClean);
		
		// displayText/snippet ($10)
		sb.append(e.getDisplayText());
		sb.append(delimClean);
		
		// event type ($11)
		sb.append(e.getEventType());
		sb.append(delimClean);
		
		// validAfter ($12)
		sb.append(e.getValidAfter());
		sb.append(delimClean);
		
		// validBefore ($13)
		sb.append(e.getValidBefore());
		sb.append(delimClean);
		
		// parentEncounterUUID($14)
		sb.append(e.getParentEncounterID()==null?"":e.getParentEncounterID().toString());
		sb.append((suffix==null?"":suffix));		
		
		return sb.toString();		
	}

}
