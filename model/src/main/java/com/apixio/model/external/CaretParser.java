package com.apixio.model.external;

import com.apixio.model.patient.ExternalID;
import com.apixio.model.patient.ClinicalCode;

import java.util.*;

/**
 * CDV (CARET Delimited Values) Parser.
 *
 * Parsing rules:
 *
 * - CARET character ('^') serves as a separator for parts of a complex value serialized into a character string.
 * - Value can come in a short form or a long form.
 * - In a short form the first part only of a complex value is present and there is no CARET chars.
 * - In a long form all parts of a complex value must be present delimited by CARET chars.
 * - Parts consisting of white space are considered empty.
 * - Short form is allowed only if first part alone is sufficient to represent a complete complex value. Otherwise an error is thrown.
 *
 * TODO: This class should only contain AXM parsing logic. Move APO parsing logic outside of this class into a different package (not `external`).
 */
public class CaretParser {

  public static String toString(AxmExternalId eid) {
    if (eid.getId() == null) {
      return eid.getAssignAuthority();
    } else {
      return eid.getId() + "^^" + eid.getAssignAuthority();
    }
  }

  public static ExternalID toExternalID(String str) throws CaretParserException {
    AxmExternalId axm = toAxmExternalId(str);
    ExternalID eid = new ExternalID();
    eid.setId(axm.getId());
    eid.setAssignAuthority(axm.getAssignAuthority());
    return eid;
  }

  /**
   * If external id 'id' part is null that it means that data came without an assign authority.
   *
   * @see #parseExternalId
   */
  public static AxmExternalId toAxmExternalIdWithAuthority(String str) throws CaretParserException {
    AxmExternalId eid = toAxmExternalId(str);
    if (eid.getId() == null) {
      throw new CaretParserException("AxmExternalId.assignAuthority is empty");
    }
    return eid;
  }

  /**
   * Having external id delimited by double carets is a legacy heritage.
   * There used to be a third middle consituent that was dropped since then.
   *
   * If external id is a plain value then it goes into AxmExternalId.assignAuthority for historical reasons.
   */
  public static AxmExternalId toAxmExternalId(String str) throws CaretParserException {
    AxmExternalId eid = new AxmExternalId();

    String[] parts = str.split("\\^");
    if (parts.length == 1) {
      String authority = parts[0].trim();
      if (authority.isEmpty()) {
        throw new CaretParserException("AxmExternalId is empty");
      }
      eid.setAssignAuthority(authority);
    } else if (parts.length == 3) {
      String id = parts[0].trim();
      if (id.isEmpty()) {
        throw new CaretParserException("AxmExternalId.id is empty");
      }
      eid.setId(id);

      // parts[1] is ignored for historical reasons

      String authority = parts[2].trim();
      if (authority.isEmpty()) {
        throw new CaretParserException("AxmExternalId.assignAuthority is empty");
      }
      eid.setAssignAuthority(authority);
    } else {
      throw new CaretParserException("AxmExternalId has 1 or more than 2 carets");
    }

    return eid;
  }

  public static List<AxmExternalId> toAxmExternalIds(String str) throws CaretParserException {
      List<AxmExternalId> externalIds = new ArrayList<>();
      String[] ids = str.split(";");
      for (String id : ids) {
          externalIds.add(toAxmExternalId(id));
      }
      return externalIds;
  }


  public static String toString(AxmClinicalCode axm) {
    StringBuilder b = new StringBuilder();

    if (axm.getName() != null) {
      b.append(axm.getName());
    }
    b.append("^");

    if (axm.getCode() != null) {
      b.append(axm.getCode());
    }
    b.append("^");

    if (axm.getSystem() != null) {
      b.append(axm.getSystem());
    }
    b.append("^");

    if (axm.getSystemOid() != null) {
      b.append(axm.getSystemOid());
    }
    b.append("^");

    if (axm.getSystemVersion() != null) {
      b.append(axm.getSystemVersion());
    }

    return b.toString();
  }

  public static ClinicalCode toClinicalCode(String str) throws CaretParserException {
    AxmCodeOrName codeOrName = toAxmCodeOrName(str);
    ClinicalCode ccode = new ClinicalCode();
    if (codeOrName.isCode()) {
        AxmClinicalCode axm = codeOrName.getCode();
        ccode.setDisplayName(axm.getName());
        ccode.setCode(axm.getCode());
        ccode.setCodingSystem(axm.getSystem());
        ccode.setCodingSystemOID(axm.getSystemOid());
        ccode.setCodingSystemVersions(axm.getSystemVersion());
    } else {
        ccode.setDisplayName(codeOrName.getName());
    }
    return ccode;
  }

  /**
   * AxmClinicalCode#codingSystem is required.
   */
  public static AxmClinicalCode toAxmClinicalCode(String str) throws CaretParserException {
    AxmClinicalCode ccode = new AxmClinicalCode();

    String[] parts = str.split("\\^", 5);

    if (parts.length != 5) {
      throw new CaretParserException("AxmClinicalCode must have 5 parts but had " + parts.length);
    }

    if (parts[parts.length - 1].contains("^")) {
      throw new CaretParserException("AxmClinicalCode must have 5 parts but had more");
    }

    String name = parts[0].trim();
    if (!name.isEmpty()) {
      ccode.setName(name);
    }

    String code = parts[1].trim();
    if (!code.isEmpty()) {
        ccode.setCode(code);
    }

    String system = parts[2].trim();
    if (!system.isEmpty()) {
        ccode.setSystem(system);
    }

    String oid = parts[3].trim();
    if (!oid.isEmpty()) {
        ccode.setSystemOid(oid);
    } else {
        throw new CaretParserException("AxmClinicalCode must have an associated coding system oid");
    }

    String version = parts[4].trim();
    if (!version.isEmpty()) {
        ccode.setSystemVersion(version);
    }

    return ccode;
  }

  public static AxmPhoneNumber toAxmPhoneNumber(String str) throws CaretParserException {
    String[] parts = str.split("\\^");

    if (parts[0].trim().isEmpty()) {
      throw new CaretParserException("AxmPhoneNumber.number is missing");
    }

    AxmPhoneNumber phone = new AxmPhoneNumber();
    phone.setNumber(parts[0]);

    if (parts.length > 1) {
      String type = parts[1].trim();
      if (!type.isEmpty()) {
        phone.setPhoneType(AxmPhoneType.valueOf(type));
      }
    }

    return phone;
  }

  public static AxmProviderInRole toAxmProviderInRole(String str) throws CaretParserException {
    String[] parts = str.split("\\^");
    if (parts.length != 4) {
        throw new CaretParserException("provider in role must <id^^authority^role> format");
    }
    AxmProviderInRole provider = new AxmProviderInRole();
    AxmExternalId providerId = toAxmExternalId(parts[0] + "^^" + parts[2]);
    provider.setProviderId(providerId);

    // parts[1] is ignore for historical purposes

    AxmActorRole role = AxmActorRole.valueOf(parts[3]);
    provider.setActorRole(role);

    return provider;
  }

  public static List<AxmProviderInRole> toAxmProviderInRoles(String str) throws CaretParserException {
    List<AxmProviderInRole> result = new ArrayList<>();
    String[] providers = str.split(";");
    for (String providerSpec : providers) {
        result.add(toAxmProviderInRole(providerSpec));
    }
    return result;
  }

  public static String flattenMap(Map<String, String> map) {
      StringBuilder b = new StringBuilder();
      for (Map.Entry<String, String> entry : map.entrySet()) {
          b.append(entry.getKey()).append('=').append(entry.getValue()).append(';');
      }
      if (b.length() > 0) {
          b.deleteCharAt(b.length() - 1); // remove trailing semicolon
      }
      return b.toString();
  }

  public static Map<String, String> parseMetadata(String str) throws CaretParserException {
    Map<String, String> metadata = new HashMap<>();

    String[] tags = str.split(";");

    for (String tag : tags) {
      String[] keyValue = tag.split("=", 2);

      if (keyValue.length != 2) {
          throw new CaretParserException("Metatag does not contain an 'equals' sign");
      }

      if (keyValue[1].isEmpty()) {
        // ignore metatag if it has no value
        continue;
      }

      if (keyValue[1].contains("=")) {
        throw new CaretParserException("Metatag contains more than 1 'equals' signs");
      }

      metadata.put(keyValue[0], keyValue[1]);
    }

    return metadata;
  }

  public static List<AxmClinicalCode> toAxmClinicalCodes(String str) throws CaretParserException {
    List<AxmClinicalCode> result = new ArrayList<>();
    String[] codes = str.split(";");
    for (String code : codes) {
      result.add(toAxmClinicalCode(code));
    }
    return result;
  }

  public static AxmCodeOrName toAxmCodeOrName(String str) throws CaretParserException {
      if (str.contains("^")) {
          AxmClinicalCode code = toAxmClinicalCode(str);
          return new AxmCodeOrName(code);
      } else {
          return new AxmCodeOrName(str);
      }
  }

  public static List<AxmCodeOrName> toAxmCodeOrNames(String str) throws CaretParserException {
    List<AxmCodeOrName> result = new ArrayList<>();
    String[] codeOrNames = str.split(";");
    for (String codeOrName : codeOrNames) {
      result.add(toAxmCodeOrName(codeOrName));
    }
    return result;
  }


}
