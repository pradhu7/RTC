package com.apixio.model.utility;

import java.io.IOException;

import org.joda.time.DateTime;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;

public class ApixioDateSerializer extends com.fasterxml.jackson.databind.JsonSerializer<DateTime> {
	
	@Override
	public void serialize(DateTime value, JsonGenerator jgen,
			SerializerProvider provider) throws IOException,
			JsonProcessingException 
	{
		jgen.writeString(value.toString());
	}

}
