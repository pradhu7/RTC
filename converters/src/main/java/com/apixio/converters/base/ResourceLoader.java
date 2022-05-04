package com.apixio.converters.base;

import java.io.InputStream;

public class ResourceLoader {

	public static final int BUFSIZE = 8192;
	public static final ResourceLoader INSTANCE = new ResourceLoader();
	
	/**
	   * This method will return the path of the specified resource 
	   * @param name the name of the resource
	   * @return stream of the resource
	   */
	public static InputStream getResourceStream(String name) {
	      InputStream ins = null;
	      try {
	          ins = INSTANCE.getClass().getClassLoader().getResourceAsStream(name);          

	      } catch (Exception e) {
	          return null;
	      } 
	      
	      return ins;
	  }
	
}
