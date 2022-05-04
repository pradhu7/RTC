/*
 * 
 */
package com.apixio.model.metric;

// TODO: Auto-generated Javadoc
/**
 * The Enum ObjectTypeEnum.
 * @author Dan Dreon
 * @version 0.1
 */
@Deprecated
public enum ObjectTypeEnum {
  
  /** The project. */
  PROJECT("PROJECT"),
  
  /** The coder. */
  CODER("CODER"),
  
  /** The hcc. */
  HCC("HCC");
  
  /** The value. */
  private final String value;
  
  /**
   * Instantiates a new object type enum.
   *
   * @param value the value
   */
  private ObjectTypeEnum(String value) {
    this.value = value;
  }
  
  /* (non-Javadoc)
     * @see java.lang.Enum#toString()
     */
  @Override
  public String toString() {
    return this.value;
  }
}
