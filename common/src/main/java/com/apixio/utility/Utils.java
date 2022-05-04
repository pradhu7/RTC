package com.apixio.utility;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.lang3.StringUtils;

public class Utils {
	
	public static List<String> convertStringToList (String str, String separator) 
	{
		List<String> result = new ArrayList<String>();
		if (str != null)
		{
			StringTokenizer tokenizer = new StringTokenizer(str, separator);
			
			while (tokenizer.hasMoreTokens())
			{
				String token = tokenizer.nextToken().toLowerCase().trim();
				if (token != null && token.length() > 0)
					result.add(token);
			}
		}
		return result;
	}
	
	public static String getTokenSeparatedString(@SuppressWarnings("rawtypes") List Ids, String separator) {
		String convertedStr = null;
		String sep = ",";
		if (separator != null && !separator.equals(""))
			sep = separator;
		if (Ids != null && !Ids.isEmpty()) {
			for (Object id : Ids) {
				if (convertedStr == null) {
					convertedStr = id.toString();
				} else {
					convertedStr += sep + " " + id;
				}
			}
		}

		return convertedStr;
	}
	
	public static String getCommaSeparatedValue(@SuppressWarnings("rawtypes") List Ids) {

		return getTokenSeparatedString(Ids, null);
	}
	
	/**
	 * @author Jack
	 * @modifiedBy Nethra Compare approximately two strings using the LevenStein metric. This metric scores -1 for each character mismatch.
	 * @param s1
	 *            the first String
	 * @param s2
	 *            the second String
	 * @return a 0 to 10 score of the fitness of match. 0 denotes no match, 10 denotes perfect.
	 */
	public static int approximateStringComparison(String s1, String s2, int threshold) {
		/*
		 * If threshold says 10 (exact match), NULLs are not equal. If non-null strings are not equal, then no point in calculating score. Just straight
		 * give rank as 0 saying the strings are not equal.
		 */
		if (threshold == 10) {
			if (!StringUtils.isBlank(s1) && !StringUtils.isBlank(s2) && s1.trim().equalsIgnoreCase(s2.trim())) {
				return 10;
			} else {
				return 0;
			}
		}
		s1 = s1 == null ? "" : s1.toLowerCase().trim();
		s2 = s2 == null ? "" : s2.toLowerCase().trim();

		/* If both the strings are equal, give rank as 10. */
		if (s1.equals(s2)) {
			return 10;
		}
		/*
		 * If threshold doesn't ask for exact match, i.e it's not 10, and the strings are not equal too, then try to calculate some rank to show how well
		 * the strings match or how different they are.
		 */

		/* If one of the strings is null or empty, then give the score as 9. (Null strings are already converted to empty strings) */
		if (s1.equals("") || s2.equals(""))
			return 9;

		/* Not doing levenstein now. So removing that code and just returning 0 if none of the above conditions are met. */
		return 0;

	}
	
	public static String changeDateFormat(String date, String fromDateFormat, String toDateFormat) throws ParseException 
	{
        
        SimpleDateFormat inputFormat = new SimpleDateFormat(fromDateFormat);
        SimpleDateFormat outputFormat = new SimpleDateFormat(toDateFormat);
        
        Date d = inputFormat.parse(date);
        
        return outputFormat.format(d);
      
    }
	
	public static <T> Set<T> addItems(Set<T> toSet, Set<T> fromSet) throws Exception 
	{
		
		if(toSet==null && fromSet!=null)
			return fromSet;
	
		if(toSet!=null && fromSet!=null)
		{
			toSet.addAll(fromSet);
		}
		
		return toSet;
	}
	
	public static <T> List<T> addItems(List<T> toList, List<T> fromList) throws Exception 
	{
		
		if(toList==null && fromList!=null)
			return fromList;
	
		if (toList!=null && fromList!=null)
		{
			Set<T> uniqueList = new HashSet<T>();
			uniqueList.addAll(toList);
			uniqueList.addAll(fromList);
			
			toList = new ArrayList<T>(uniqueList);
		}
		
		return toList;
	}
	
	public static <T> void mergeProperties(T toEntity, T fromEntity) throws Exception 
	{
		if (toEntity!=null && fromEntity!=null)
		{
			for (Field field : toEntity.getClass().getDeclaredFields())
			{
	            field.setAccessible(true);
				if (!(field.getGenericType() instanceof ParameterizedType))
				{
					if (field.get(fromEntity) != null && !field.get(fromEntity).toString().trim().equals(""))
					{
						field.set(toEntity, field.get(fromEntity));
					}
				}
				
			}
		}
		else
		{
			throw new Exception("Merge properties fails. Parameters given is/are null.");
		}
	}

}
