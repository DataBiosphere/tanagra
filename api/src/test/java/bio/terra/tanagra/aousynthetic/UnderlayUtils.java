package bio.terra.tanagra.aousynthetic;

import com.google.common.collect.ImmutableList;
import java.util.List;

/**
 * Utilities for testing with the AoU synthetic underlay. These constants are used to generate SQL
 * for queries.
 */
public final class UnderlayUtils {
  private UnderlayUtils() {}

  public static final String UNDERLAY_NAME = "aou_synthetic";

  public static final String PERSON_ENTITY = "person";
  public static final String PERSON_ID_ATTRIBUTE = "person_id";
  public static final String PERSON_YEAROFBIRTH_ATTRIBUTE = "year_of_birth";
  public static final String PERSON_ETHNICITY_ATTRIBUTE = "ethnicity_concept_id";
  public static final String PERSON_GENDER_ATTRIBUTE = "gender_concept_id";
  public static final String PERSON_GENDER_NAME_ATTRIBUTE = "gender";
  public static final String PERSON_RACE_ATTRIBUTE = "race_concept_id";
  public static final String PERSON_RACE_NAME_ATTRIBUTE = "race";
  public static final String PERSON_SEXATBIRTH_ATTRIBUTE = "sex_at_birth_concept_id";
  public static final List<String> ALL_PERSON_ATTRIBUTES =
      ImmutableList.of(
          "person_id",
          "gender_concept_id",
          "gender",
          "race_concept_id",
          "race",
          "ethnicity_concept_id",
          "ethnicity",
          "sex_at_birth_concept_id",
          "sex_at_birth");

  public static final String CONDITION_ENTITY = "condition";
  public static final String CONDITION_ENTITY_PERSON_COUNT_ATTRIBUTE = "person_count";
  public static final List<String> ALL_CONDITION_ATTRIBUTES =
      ImmutableList.of(
          "concept_id",
          "concept_name",
          "vocabulary_id",
          "vocabulary_name",
          "standard_concept",
          "concept_code");
  public static final String CONDITION_HIERARCHY_PATH_ATTRIBUTE = "t_path_concept_id";
  public static final String CONDITION_HIERARCHY_NUMCHILDREN_ATTRIBUTE = "t_numChildren_concept_id";

  public static final String CONDITION_OCCURRENCE_ENTITY = "condition_occurrence";
  public static final List<String> ALL_CONDITION_OCCURRENCE_ATTRIBUTES =
      ImmutableList.of(
          "condition_occurrence_id",
          "person_id",
          "condition_concept_id",
          "condition_start_date",
          "condition_end_date",
          "stop_reason",
          "visit_occurrence_id",
          "condition_source_value",
          "condition_source_concept_id");

  public static final String PROCEDURE_ENTITY = "procedure";
  public static final List<String> ALL_PROCEDURE_ATTRIBUTES =
      ImmutableList.of(
          "concept_id",
          "concept_name",
          "vocabulary_id",
          "vocabulary_name",
          "standard_concept",
          "concept_code");
  public static final String PROCEDURE_HIERARCHY_PATH_ATTRIBUTE = "t_path_concept_id";
  public static final String PROCEDURE_HIERARCHY_NUMCHILDREN_ATTRIBUTE = "t_numChildren_concept_id";

  public static final String PROCEDURE_OCCURRENCE_ENTITY = "procedure_occurrence";
  public static final List<String> ALL_PROCEDURE_OCCURRENCE_ATTRIBUTES =
      ImmutableList.of(
          "procedure_occurrence_id",
          "person_id",
          "procedure_concept_id",
          "procedure_date",
          "visit_occurrence_id",
          "procedure_source_value",
          "procedure_source_concept_id");

  public static final String INGREDIENT_ENTITY = "ingredient";
  public static final List<String> ALL_INGREDIENT_ATTRIBUTES =
      ImmutableList.of(
          "concept_id",
          "concept_name",
          "vocabulary_id",
          "vocabulary_name",
          "standard_concept",
          "concept_code");
  public static final String INGREDIENT_HIERARCHY_PATH_ATTRIBUTE = "t_path_concept_id";
  public static final String INGREDIENT_HIERARCHY_NUMCHILDREN_ATTRIBUTE =
      "t_numChildren_concept_id";

  public static final String INGREDIENT_OCCURRENCE_ENTITY = "ingredient_occurrence";
  public static final List<String> ALL_INGREDIENT_OCCURRENCE_ATTRIBUTES =
      ImmutableList.of(
          "drug_exposure_id",
          "person_id",
          "drug_concept_id",
          "drug_exposure_start_date",
          "drug_exposure_end_date",
          "stop_reason",
          "refills",
          "days_supply",
          "visit_occurrence_id",
          "drug_source_value",
          "drug_source_concept_id");

  public static final String BRAND_ENTITY = "brand";
  public static final List<String> ALL_BRAND_ATTRIBUTES =
      ImmutableList.of("concept_id", "concept_name", "standard_concept", "concept_code");

  public static final String MEASUREMENT_ENTITY = "measurement";
  public static final List<String> ALL_MEASUREMENT_ATTRIBUTES =
      ImmutableList.of(
          "concept_id",
          "concept_name",
          "vocabulary_id",
          "vocabulary_name",
          "standard_concept",
          "concept_code");
  public static final String MEASUREMENT_HIERARCHY_PATH_ATTRIBUTE = "t_path_concept_id";
  public static final String MEASUREMENT_HIERARCHY_NUMCHILDREN_ATTRIBUTE =
      "t_numChildren_concept_id";

  public static final String MEASUREMENT_OCCURRENCE_ENTITY = "measurement_occurrence";
  public static final List<String> ALL_MEASUREMENT_OCCURRENCE_ATTRIBUTES =
      ImmutableList.of(
          "measurement_id",
          "person_id",
          "measurement_concept_id",
          "measurement_date",
          "value_as_concept_id",
          "unit_concept_id",
          "visit_occurrence_id",
          "measurement_source_value",
          "measurement_source_concept_id");

  public static final String VISIT_ENTITY = "visit";
  public static final List<String> ALL_VISIT_ATTRIBUTES =
      ImmutableList.of("concept_id", "concept_name");

  public static final String VISIT_OCCURRENCE_ENTITY = "visit_occurrence";
  public static final List<String> ALL_VISIT_OCCURRENCE_ATTRIBUTES =
      ImmutableList.of(
          "visit_occurrence_id",
          "person_id",
          "visit_concept_id",
          "visit_start_date",
          "visit_end_date",
          "visit_source_value",
          "visit_source_concept_id");

  public static final String OBSERVATION_ENTITY = "observation";
  public static final List<String> ALL_OBSERVATION_ATTRIBUTES =
      ImmutableList.of(
          "concept_id",
          "concept_name",
          "vocabulary_id",
          "vocabulary_name",
          "standard_concept",
          "concept_code");

  public static final String OBSERVATION_OCCURRENCE_ENTITY = "observation_occurrence";
  public static final List<String> ALL_OBSERVATION_OCCURRENCE_ATTRIBUTES =
      ImmutableList.of(
          "observation_id",
          "person_id",
          "observation_concept_id",
          "observation_date",
          "value_as_string",
          "value_as_concept_id",
          "unit_concept_id",
          "visit_occurrence_id",
          "observation_source_value",
          "observation_source_concept_id");

  public static final String DEVICE_ENTITY = "device";
  public static final List<String> ALL_DEVICE_ATTRIBUTES =
      ImmutableList.of(
          "concept_id",
          "concept_name",
          "vocabulary_id",
          "vocabulary_name",
          "standard_concept",
          "concept_code");

  public static final String DEVICE_OCCURRENCE_ENTITY = "device_occurrence";
  public static final List<String> ALL_DEVICE_OCCURRENCE_ATTRIBUTES =
      ImmutableList.of(
          "device_exposure_id",
          "person_id",
          "device_concept_id",
          "device_exposure_start_date",
          "device_exposure_end_date",
          "visit_occurrence_id",
          "device_source_value",
          "device_source_concept_id");
}
