package com.apixio.model.commonparser;

/**
 * The string deserializer converts a string to object of type T
 * @author vvyas
 *
 */
public interface StringDeserializer<T> {
	public T fromString(String s);
}
