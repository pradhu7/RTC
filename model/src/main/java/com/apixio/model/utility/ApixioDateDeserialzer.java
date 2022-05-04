package com.apixio.model.utility;

import java.io.IOException;
import java.text.ParseException;

import org.apache.commons.lang.time.DateUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import com.fasterxml.jackson.databind.JsonDeserializer;

public class ApixioDateDeserialzer extends JsonDeserializer<DateTime> 
	implements StringDeserializer<DateTime> {
	
	// this is  a collection of date patterns that we usually encounter in our
	// data to try and parse it out.
	private final String [] datePatterns = new String [] {
			"yyyyMMdd",
			"yyyyMMddkkmmssZ",
			"yyyyMMddkkmmss",			
	};
	
	@Override
	public DateTime getNullValue() { return null; }

	private DateTime parseDateTime(String date) 
	{
		if(date == null || date.trim().length() <= 0)
			return null;
		
		try 
		{			
			return new DateTime(DateUtils.parseDate(date, datePatterns));			
		} 
		catch (ParseException e) 
		{
			// this happens when the date was not parsable by our default date time parser
			// so we need to try our other parser.
			try 
			{
				DateTimeFormatter fmt = ISODateTimeFormat.dateTimeParser();
				return fmt.parseDateTime(date);
			} 
			catch (IllegalArgumentException exc) 
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			}
		}					
	}

	public DateTime fromString(String s) 
	{
		return parseDateTime(s);
	}

	@Override
	public DateTime deserialize(com.fasterxml.jackson.core.JsonParser jp,
			com.fasterxml.jackson.databind.DeserializationContext ctxt)
			throws IOException,
			com.fasterxml.jackson.core.JsonProcessingException 
			{
		if(jp == null)
			return null;
		
		String date = jp.getText();
		
		if (date  == null || date.trim().length() <= 0)
			return null;
		
		try 
		{			
			return new DateTime(DateUtils.parseDate(date, datePatterns));			
		} 
		catch (ParseException e) 
		{
			// this happens when the date was not parsable by our default date time parser
			// so we need to try our other parser.
			try 
			{
				DateTimeFormatter fmt = ISODateTimeFormat.dateTimeParser();
				return fmt.parseDateTime(date);
			} 
			catch (IllegalArgumentException exc) 
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			}
		}	
	}
}
