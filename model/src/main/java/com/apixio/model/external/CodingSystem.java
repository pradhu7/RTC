package com.apixio.model.external;

/**
 * Coding systems with a universally recognized object identifiers (oid).
 * @see http://www.oid-info.com/
 */
public enum CodingSystem {

  ICD_9CM_DIAGNOSIS_CODES("2.16.840.1.113883.6.103"),
  ICD_9CM_PROCEDURE_CODES("2.16.840.1.113883.6.104"),
  ICD_10CM_DIAGNOSIS_CODES("2.16.840.1.113883.6.90"),
  CPT_4("2.16.840.1.113883.6.12");

  private final String oid;

  private CodingSystem(String oid) {
    this.oid = oid;
  }

  public String getOid() {
    return oid;
  }

  public static CodingSystem byOid(String oid) {
    for (CodingSystem system : CodingSystem.values()) {
        if (system.oid.equals(oid)) {
            return system;
        }
    }
    return null;
  }
}
