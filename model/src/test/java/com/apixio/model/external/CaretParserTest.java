package com.apixio.model.external;

import com.apixio.model.patient.ClinicalCode;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class CaretParserTest {

  @Test
  public void complex_strings_can_be_parsed_into_axm_clinical_codes() throws CaretParserException {
    AxmClinicalCode expected = new AxmClinicalCode();
    expected.setName("DIABETES INSIPIDUS");
    expected.setCode("253.5");
    expected.setSystem("ICD_9CM_DIAGNOSES_CODES");
    expected.setSystemOid("2.16.840.1.113883.6.103");

    AxmClinicalCode actual = CaretParser.toAxmClinicalCode("DIABETES INSIPIDUS^253.5^ICD_9CM_DIAGNOSES_CODES^2.16.840.1.113883.6.103^");
    assertEquals(expected, actual);
  }

  @Test(expected = CaretParserException.class)
  public void simple_strings_cannot_be_parsed_into_axm_clinical_codes() throws CaretParserException {
    CaretParser.toAxmClinicalCode("DIABETES INSIPIDUS");
  }

  @Test
  public void complex_strings_can_be_parsed_into_apo_clinical_codes() throws CaretParserException {
      ClinicalCode expected = new ClinicalCode();
      expected.setDisplayName("DIABETES INSIPIDUS");
      expected.setCode("253.5");
      expected.setCodingSystem("ICD_9CM_DIAGNOSES_CODES");
      expected.setCodingSystemOID("2.16.840.1.113883.6.103");

      ClinicalCode actual = CaretParser.toClinicalCode("DIABETES INSIPIDUS^253.5^ICD_9CM_DIAGNOSES_CODES^2.16.840.1.113883.6.103^");

      assertEquals(expected, actual);
  }

  @Test
  public void simple_strings_can_be_parsed_into_apo_clinical_codes() throws CaretParserException {
      ClinicalCode expected = new ClinicalCode();
      expected.setDisplayName("DIABETES INSIPIDUS");

      ClinicalCode actual = CaretParser.toClinicalCode("DIABETES INSIPIDUS");

      assertEquals(expected, actual);
  }

  @Test
  public void axm_clinical_codes_may_consist_of_system_oid_only() throws CaretParserException {
    CaretParser.toAxmClinicalCode("^^^2.16.840.1.113883.6.103^");
  }

  @Test(expected=CaretParserException.class)
  public void fails_when_clinical_code_misses_system_oid() throws CaretParserException {
    CaretParser.toAxmClinicalCode("^253.5^ICD_9CM_DIAGNOSES_CODES^^");
  }


  @Test(expected=CaretParserException.class)
  public void fails_when_clinical_code_has_more_than_five_parts() throws CaretParserException {
    CaretParser.toAxmClinicalCode("1^2^3^4^5^6");
  }

  @Test(expected=CaretParserException.class)
  public void fails_when_metatag_has_multiple_equal_signs() throws CaretParserException {
    CaretParser.parseMetadata("a=b=c");
  }

  @Test(expected=CaretParserException.class)
  public void fails_when_metatag_has_no_equal_signs() throws CaretParserException {
    CaretParser.parseMetadata("a");
  }

  @Test
  public void can_parse_metadata() throws CaretParserException {
      Map<String, String> expected = new HashMap<>();
      expected.put("k1", "v1");
      expected.put("k2", "v2");

      Map<String, String> actual = CaretParser.parseMetadata("k1=v1;k2=v2");

      assertEquals(expected, actual);
  }

  @Test
  public void ignores_metatags_with_no_value() throws CaretParserException {
      Map<String, String> expected = new HashMap<>();
      expected.put("k1", "v1");

      Map<String, String> actual = CaretParser.parseMetadata("k1=v1;k2=");

      assertEquals(expected, actual);
  }

  @Test
  public void can_parse_provider_in_roles() throws CaretParserException {
      AxmProviderInRole actual = CaretParser.toAxmProviderInRole("id^^authority^AUTHORING_PROVIDER");

      assertEquals("id", actual.getProviderId().getId());
      assertEquals("authority", actual.getProviderId().getAssignAuthority());
      assertSame(AxmActorRole.AUTHORING_PROVIDER, actual.getActorRole());
  }

  @Test(expected=CaretParserException.class)
  public void fails_when_there_is_no_role() throws CaretParserException {
      CaretParser.toAxmProviderInRole("id^^authority");
  }
}
