package com.apixio.model.utility;

import java.io.IOException;

import com.apixio.model.EitherStringOrNumber;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;

public class EitherStringOrNumberSerializer extends com.fasterxml.jackson.databind.JsonSerializer<EitherStringOrNumber> {
	
	@Override
	public void serialize(EitherStringOrNumber value, JsonGenerator jgen,
			SerializerProvider provider) throws IOException,
			JsonProcessingException 
	{
		if(value!=null && value.left() != null) 
		{
			jgen.writeString(value.left());
		} 
		else if(value!=null && value.right() != null)
		{
			jgen.writeNumber(value.right().doubleValue());
		}
		else
		    jgen.writeString("");
	}
}
