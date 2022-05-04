/*
 * 
 */
package com.apixio.model.metric;

import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.apixio.model.event.ReferenceType;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

// TODO: Auto-generated Javadoc
/**
 * The Class Metric.
 * @author Dan Dreon
 * @version 0.1
 */
public class Metric implements Serializable {
  
  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = -5363532069169965177L;

  /** The object */
  private ReferenceType object;

  /** The group */
  private ReferenceType group;
  
  /** The metric type. */
  private String metricType;

  /** The period. */
  private long period;
  
  /** The timestamp. */
  private long timestamp;
  
  /** The metric. */
  private double metric;
  
  /** The attributes. */
  private Map<String,String> attributes = new HashMap<>();
  
  /**
   * Instantiates a new metric.
   */
  public Metric() {
  }
  
  /**
   * Instantiates a new metric.
   *
   * @param builder the builder
   */
  private Metric(Builder builder) {
    this.object = new ReferenceType();
    this.object.setUri(builder.objectId);
    this.object.setType(builder.objectType);

    this.group = new ReferenceType();
    this.group.setUri(builder.groupId);
    this.group.setType(builder.groupType);

    this.metricType = builder.metricType;
    this.period = builder.period;
    this.timestamp = builder.timestamp;
    
    this.metric = builder.metric;
    this.attributes = builder.attributes;
  }
  
  /**
   * Gets the object.
   *
   * @return the object
   */
  public ReferenceType getObject() {
    return object;
  }

  /**
   * Gets the group.
   *
   * @return the group
   */
  public ReferenceType getGroup() {
    return group;
  }

  /**
   * Gets the metric type.
   *
   * @return the metric type
   */
  public String getMetricType() {
    return metricType;
  }

  /**
   * Gets the period type.
   *
   * @return the period type
   */
  public long getPeriod() {
    return period;
  }

  /**
   * Gets the timestamp.
   *
   * @return the timestamp
   */
  public long getTimestamp() {
    return timestamp;
  }

  /**
   * Gets the metric.
   *
   * @return the metric
   */
  public double getMetric() {
    return metric;
  }

  /**
   * Gets the attributes.
   *
   * @return the attributes
   */
  public Map<String, String> getAttributes() {
    return attributes;
  }
  
  /**
   * The Class Builder.
   */
  public static class Builder {
    
    /** The object id. */
    private String objectId;

    /** The object type. */
    private String objectType;
    
    /** The group id. */
    private String groupId;
    
    /** The group type. */
    private String groupType;
    
    /** The metric type. */
    private String metricType;
    
    /** The period type. */
    private long period;
    
    /** The timestamp. */
    private long timestamp;
    
    /** The metric. */
    private double metric;
    
    /** The attributes. */
    private Map<String,String> attributes = new HashMap<>();
    
    /**
     * Sets the object id.
     *
     * @param objectId the object id
     * @return the builder
     */
    public Builder setObjectId(String objectId) {
      this.objectId = objectId;
      return this;
    }
    
    /**
     * Sets the object type.
     *
     * @param objectType the object type
     * @return the builder
     */
    public Builder setObjectType(String objectType) {
      this.objectType = objectType;
      return this;
    }
    
    /**
     * Sets the group id.
     *
     * @param groupId the group id
     * @return the builder
     */
    public Builder setGroupId(String groupId) {
      this.groupId = groupId;
      return this;
    }
    
    /**
     * Sets the group type.
     *
     * @param groupType the group type
     * @return the builder
     */
    public Builder setGroupType(String groupType) {
      this.groupType = groupType;
      return this;
    }

    /**
     * Sets the metric type.
     *
     * @param metricType the metric type
     * @return the builder
     */
    public Builder setMetricType(String metricType) {
      this.metricType = metricType;
      return this;
    }
    
    /**
     * Sets the period type.
     *
     * @param periodType the period type
     * @return the builder
     */
    public Builder setPeriod(long period) {
      this.period = period;
      return this;
    }

    /**
     * Sets the timestamp.
     *
     * @param timestamp the timestamp
     * @return the builder
     */
    public Builder setTimestamp(long timestamp) {
      this.timestamp = timestamp;
      return this;
    }

    /**
     * Sets the metric.
     *
     * @param metric the metric
     * @return the builder
     */
    public Builder setMetric(double metric) {
      this.metric = metric;
      return this;
    }
    
    /**
     * Sets the attribute.
     *
     * @param key the key
     * @param value the value
     * @return the builder
     */
    public Builder setAttribute(String key, String value) {
      this.attributes.put(key, value);
      return this;
    }
    
    /**
     * Builds the.
     *
     * @return the metric
     */
    public Metric build() {
      return new Metric(this);
    }
    
    /**
     * Builds the.
     *
     * @param json the json
     * @return the metric
     * @throws JsonParseException the json parse exception
     * @throws JsonMappingException the json mapping exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public Metric build(String json) throws JsonParseException, JsonMappingException, IOException {
      return new MetricJSONParser().parseMetricTypeData(json);
    }
  }
  
  /**
   * The main method.
   *
   * @param args the arguments
   */
  public static final void main(String[] args) {
    Metric m = new Metric.Builder()
      .setObjectId("PRJ10000321A")
      .setObjectType("project")
      .setMetricType("OPPS_ACCEPTED")
      .setPeriod(60 * 60 * 1000L)
      .setTimestamp(new Date().getTime())
      .setGroupType("coder")
      .setGroupId("Coder123")
      .setMetric(12)
      .setAttribute("ORG", "10000321")
      .setAttribute("NAME", "2013Final")
      .build();
    
    try {
      String json = new MetricJSONParser().toJSON(m);
      System.out.println(new MetricJSONParser().toJSON(m));
      
      m = new Metric.Builder().build(json);
      System.out.println(new MetricJSONParser().toJSON(m));
      
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
}
