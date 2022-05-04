package com.apixio.model.csv;

import com.apixio.model.external.*;

import static com.apixio.model.csv.Constraint.*;
import static com.apixio.model.csv.ColumnName.*;
import static com.apixio.model.csv.Column.column;


public abstract class DataDefinition {

  public static final Column[] FILTER_COLS = {
    column(PATIENT_ID, REQUIRED, ID_WITH_AUTHORITY)
  };

  public static final Column[] XWALK_COLS = {
    column(PATIENT_ID, REQUIRED, ID_WITH_AUTHORITY),
    column(EXTERNAL_ID, REQUIRED, ID_WITH_AUTHORITY)
  };

  public static final Column[] DEMOGRAPHICS_COLS = {
    column(PATIENT_ID, REQUIRED, ID_WITH_AUTHORITY),
    column(GIVEN_NAME, REQUIRED),
    column(FAMILY_NAME, REQUIRED),
    column(PREFIX),
    column(SUFFIX),
    column(GENDER, REQUIRED, IN_ENUM(AxmGender.class)),
    column(DATE_OF_BIRTH, REQUIRED),
    column(DATE_OF_DEATH),
    column(META_DATA),
    column(EDIT_TYPE, REQUIRED, IN_ENUM(AxmEditType.class))
  };

  public static final Column[] DOCMETADATA_COLS = {
    column(PATIENT_ID, REQUIRED, ID_WITH_AUTHORITY),
    column(DOCUMENT_UUID, REQUIRED),
    column(ORIGINAL_ID, ID),
    column(TITLE),
    column(DATE),
    column(ENCOUNTER_ID),
    column(PROVIDERS),
    column(MIME_TYPE),
    column(CODE),
    column(META_DATA),
    column(EDIT_TYPE, REQUIRED, IN_ENUM(AxmEditType.class))
  };

  public static final Column[] DOCUMENTS_COLS = {
    column(PATIENT_ID, REQUIRED, ID_WITH_AUTHORITY),
    column(ORIGINAL_ID, REQUIRED, ID),
    column(TITLE, REQUIRED),
    column(DATE, REQUIRED),
    column(ENCOUNTER_ID),
    column(PROVIDERS),
    column(URI),
    column(CONTENT),
    column(MIME_TYPE, REQUIRED, IN_ENUM(AxmMimeType.class)),
    column(CODE),
    column(META_DATA),
    column(EDIT_TYPE, REQUIRED, IN_ENUM(AxmEditType.class))
  };

  public static final Column[] PROBLEMS_COLS = {
    column(PATIENT_ID, REQUIRED, ID_WITH_AUTHORITY),
    column(ORIGINAL_ID, REQUIRED, ID),
    column(PROVIDERS),
    column(NAME, REQUIRED),
    column(RESOLUTION_STATUS, IN_ENUM(AxmResolution.class)),
    column(TEMPORAL_STATUS),
    column(START_DATE, REQUIRED),
    column(END_DATE, REQUIRED),
    column(DIAGNOSIS_DATE),
    column(CODE, REQUIRED),
    column(META_DATA),
    column(EDIT_TYPE, REQUIRED, IN_ENUM(AxmEditType.class))
  };

  public static final Column[] RAPS_COLS = {
    column(PATIENT_ID, REQUIRED),
    column(ORIGINAL_ID, REQUIRED, ID),
    column(FROM_DATE, REQUIRED),
    column(THROUGH_DATE, REQUIRED),
    column(DIAGNOSIS_CODE, REQUIRED),
    column(META_DATA, REQUIRED) // we need METADATA required so that unique MySQL constraint on RAPS persistent table works.
  };

  public static final Column[] MAO_004_COLS = {
    column(PATIENT_ID, REQUIRED), // beneficiary HICN
    column(ORIGINAL_ID, REQUIRED, ID),
    column(FROM_DATE, REQUIRED), // "from" date of service
    column(THROUGH_DATE, REQUIRED), // "through" date of service
    column(DIAGNOSIS_CODE, REQUIRED),
    column(MA_CONTRACT_ID),
    column(REPORT_DATE),
    column(SUBMISSION_INTERCHANGE_NUMBER),
    column(SUBMISSION_FILE_TYPE),
    column(ENCOUNTER_ICN),
    column(REPLACEMENT_ENCOUNTER_SWITCH),
    column(ORIGINAL_ENCOUNTER_ICN),
    column(PLAN_SUBMISSION_DATE),
    column(PROCESSING_DATE),
    column(CLAIM_TYPE),
    column(ENCOUNTER_TYPE_SWITCH),
    column(ADD_OR_DELETE_FLAG),
    column(META_DATA)
  };

  public static final Column[] PROVIDERS_COLS = {
    column(PATIENT_ID, REQUIRED, ID_WITH_AUTHORITY),
    column(ORIGINAL_ID, REQUIRED, ID),
    column(ALTERNATE_IDS),
    column(GIVEN_NAME, REQUIRED),
    column(FAMILY_NAME, REQUIRED),
    column(PREFIX),
    column(SUFFIX),
    column(TITLE),
    column(ORG_NAME),
    column(EMAIL),
    column(PHONE),
    column(META_DATA),
    column(EDIT_TYPE, REQUIRED, IN_ENUM(AxmEditType.class))
  };

  public static final Column[] PRESCRIPTIONS_COLS = {
    column(PATIENT_ID, REQUIRED, ID_WITH_AUTHORITY),
    column(ORIGINAL_ID, REQUIRED, ID),
    column(PROVIDERS),
    column(GENERIC_NAME),
    column(BRAND_NAME),
    column(CODE, REQUIRED),
    column(PRESCRIPTION_DATE, REQUIRED),
    column(AMOUNT),
    column(DIRECTIONS),
    column(META_DATA),
    column(EDIT_TYPE, REQUIRED, IN_ENUM(AxmEditType.class))
  };

  public static final Column[] BIOMETRICS_COLS = {
          column(PATIENT_ID, REQUIRED, ID_WITH_AUTHORITY),
          column(ORIGINAL_ID, REQUIRED, ID),
          column(PROVIDERS),
          column(NAME, REQUIRED),
          column(VALUE, REQUIRED),
          column(UNITS),
          column(DATE),
          column(CODE, REQUIRED),
          column(META_DATA),
          column(EDIT_TYPE, REQUIRED, IN_ENUM(AxmEditType.class))
  };

  public static final Column[] SOCIAL_HISTORY_COLS = {
          column(PATIENT_ID, REQUIRED, ID_WITH_AUTHORITY),
          column(ORIGINAL_ID, REQUIRED, ID),
          column(PROVIDERS),
          column(ENCOUNTER_ID),
          column(GROUP_CODE, REQUIRED),
          column(ITEM_CODE, REQUIRED),
          column(ITEM_NAME, REQUIRED),
          column(ITEM_VALUE, REQUIRED),
          column(DATE),
          column(META_DATA),
          column(EDIT_TYPE, REQUIRED, IN_ENUM(AxmEditType.class))
  };

  public static final Column[] ENCOUNTERS_COLS = {
    column(PATIENT_ID, REQUIRED, ID_WITH_AUTHORITY),
    column(ORIGINAL_ID, REQUIRED, ID),
    column(START_DATE, REQUIRED),
    column(END_DATE, REQUIRED),
    column(CARE_SITE),
    column(PROVIDERS),
    column(CODE),
    column(META_DATA),
    column(EDIT_TYPE, REQUIRED, IN_ENUM(AxmEditType.class))
  };

  public static final Column[] LAB_RESULTS_COLS = {
    column(PATIENT_ID, REQUIRED, ID_WITH_AUTHORITY),
    column(ORIGINAL_ID, REQUIRED, ID),
    column(PROVIDERS),
    column(NAME),
    column(CODE, REQUIRED),
    column(PANEL_CODE),
    column(SAMPLE_DATE, REQUIRED),
    column(VALUE),
    column(UNITS),
    column(RESULT_RANGE),
    column(FLAG),
    column(NOTE),
    column(META_DATA),
    column(EDIT_TYPE, REQUIRED, IN_ENUM(AxmEditType.class))
  };

  public static final Column[] COVERAGE_COLS = {
    column(PATIENT_ID, REQUIRED, ID_WITH_AUTHORITY),
    column(HEALTH_PLAN_NAME, REQUIRED),
    column(START_DATE, REQUIRED),
    column(END_DATE),
    column(SUBSCRIBER_ID, REQUIRED, ID_WITH_AUTHORITY),
    column(BENEFICIARY_ID, ID_WITH_AUTHORITY),
    column(GROUP_ID, ID_WITH_AUTHORITY),
    column(CONTRACT_NUMBER, ID_WITH_AUTHORITY),
    column(SEQUENCE_NUMBER),
    column(TYPE, REQUIRED, IN_ENUM(AxmCoverageType.class)),
    column(HICN, ID_WITH_AUTHORITY),
    column(META_DATA),
    column(EDIT_TYPE, REQUIRED, IN_ENUM(AxmEditType.class))
  };

  public static final Column[] PAGES_COLS = {
    column(PATIENT_ID, REQUIRED, ID_WITH_AUTHORITY),
    column(ORIGINAL_ID, REQUIRED, ID),
    column(PAGE_ORDER, REQUIRED),
    column(TITLE, REQUIRED),
    column(DATE, REQUIRED),
    column(ENCOUNTER_ID),
    column(PROVIDERS),
    column(URI),
    column(CONTENT),
    column(MIME_TYPE, REQUIRED, IN_ENUM(AxmMimeType.class)),
    column(CODE),
    column(META_DATA),
    column(EDIT_TYPE, REQUIRED, IN_ENUM(AxmEditType.class))
  };

  public static final Column[] PROCEDURES_COLS = {
    column(PATIENT_ID, REQUIRED, ID_WITH_AUTHORITY),
    column(ORIGINAL_ID, REQUIRED, ID),
    column(PROVIDERS),
    column(NAME, REQUIRED),
    column(START_DATE, REQUIRED),
    column(END_DATE, REQUIRED),
    column(CODE, REQUIRED),
    column(INTERPRETATION),
    column(BODY_PART_CODE),
    column(DIAGNOSIS_CODES),
    column(META_DATA),
    column(EDIT_TYPE, REQUIRED, IN_ENUM(AxmEditType.class))
  };

  public static final Column[] FFS_CLAIMS_COLS = {
  column(PATIENT_ID, REQUIRED, ID_WITH_AUTHORITY),
  column(ORIGINAL_ID, REQUIRED, ID),
  column(PROVIDERS),
  column(NAME),
  column(START_DATE, REQUIRED),
  column(END_DATE, REQUIRED),
  column(CODE),
  column(INTERPRETATION),
  column(BODY_PART_CODE),
  column(DIAGNOSIS_CODES),
  column(BILL_TYPE, REQUIRED),
  column(META_DATA),
  column(EDIT_TYPE, REQUIRED, IN_ENUM(AxmEditType.class))
  };

  public static final Column[] FFS_CLAIMS_EXTENDED_COLS = {
  column(PATIENT_ID, REQUIRED, ID_WITH_AUTHORITY),
  column(ORIGINAL_ID, REQUIRED, ID),
  column(PROVIDERS),
  column(NAME),
  column(START_DATE, REQUIRED),
  column(END_DATE, REQUIRED),
  column(CODE),
  column(INTERPRETATION),
  column(BODY_PART_CODE),
  column(DIAGNOSIS_CODES),
  column(BILL_TYPE_CODE),
  column(BILLING_PROVIDER_ID),
  column(BILLING_PROVIDER_NAME),
  column(PROVIDER_TYPE_CODE),
  column(TRANSACTION_DATE, REQUIRED),
  column(DELETE_INDICATOR),
  column(META_DATA),
  column(EDIT_TYPE, REQUIRED, IN_ENUM(AxmEditType.class))
  };

  public static final Column[] HICN_HISTORY_COLS = {
    column(HICN),
    column(CORRECTED_HICN),
    column(FROM_DATE)
  };
}
