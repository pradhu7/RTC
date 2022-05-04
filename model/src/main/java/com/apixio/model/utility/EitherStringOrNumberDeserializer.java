package com.apixio.model.utility;

import java.io.IOException;

import com.apixio.model.EitherStringOrNumber;
import com.apixio.model.EitherStringOrNumber.EitherNumber;
import com.apixio.model.EitherStringOrNumber.EitherString;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonDeserializer;

public class EitherStringOrNumberDeserializer extends
		JsonDeserializer<EitherStringOrNumber> {

	@Override
	public EitherStringOrNumber deserialize(
			com.fasterxml.jackson.core.JsonParser jp,
			com.fasterxml.jackson.databind.DeserializationContext ctxt)
			throws IOException,
			com.fasterxml.jackson.core.JsonProcessingException 
	{
		if (jp.getCurrentToken().isNumeric()) 
		{
			EitherStringOrNumber d = new EitherNumber(jp.getDoubleValue());
			return d;
		} 
		else if (jp.getCurrentToken().equals(JsonToken.VALUE_STRING)) 
		{
			EitherStringOrNumber r = new EitherString(jp.getText());
			return r;
		}
		
		return null;
	}
}
