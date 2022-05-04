/*
 * 
 */
package com.apixio.model.metric;

// TODO: Auto-generated Javadoc
/**
 * The Enum MetricTypeEnum.
 * @author Dan Dreon
 * @version 0.1
 */
@Deprecated
public enum MetricTypeEnum {
  
  /** The active coders. */
  ACTIVE_CODERS("ACTIVE_CODERS"),
  
  /** The coder hours. */
  CODER_HOURS("CODER_HOURS"),
  
  /** The coder cost. */
  CODER_COST("CODER_COST"),
  
  /** The budget spent. */
  BUDGET_SPENT("BUDGET_SPENT"),

  /** The opps created. */
  OPPS_CREATED("OPPS_CREATED"),
  
  /** The opps annotated. */
  OPPS_ANNOTATED("OPPS_ANNOTATED"),
  
  /** The opps accepted. */
  OPPS_ACCEPTED("OPPS_ACCEPTED"),
  
  /** The opps rejected. */
  OPPS_REJECTED("OPPS_REJECTED"),
  
  /** The opps flagged. */
  OPPS_FLAGGED("OPPS_FLAGGED"),
  
  /** The cwi annotated. */
  CWI_ANNOTATED("CWI_ANNOTATED"),
  
  /** The cwi accepted. */
  CWI_ACCEPTED("CWI_ACCEPTED"),
  
  /** The cwi rejected. */
  CWI_REJECTED("CWI_REJECTED"),
  
  /** The cwi skipped. */
  CWI_SKIPPED("CWI_SKIPPED"),
  
  /** The cwi remaining. */
  CWI_REMAINING("CWI_REMAINING"),
  
  /** The qwi annotated. */
  QWI_ANNOTATED("QWI_ANNOTATED"),
  
  /** The qwi skipped. */
  QWI_SKIPPED("QWI_SKIPPED"),
  
  /** The qwi overturned. */
  QWI_OVERTURNED("QWI_OVERTURNED");
  
  /** The value. */
  private final String value;
  
  /**
   * Constructor.
   *
   * @param value the metric type value
   */
  private MetricTypeEnum(String value) {
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
