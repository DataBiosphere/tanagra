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

  public static final String BQ_PROJECT_ID = "broad-tanagra-dev";
  public static final String BQ_DATASET_ID = "aou_synthetic_SR2019q4r4";
  public static final String BQ_DATASET_SQL_REFERENCE = BQ_PROJECT_ID + "." + BQ_DATASET_ID;

  public static final String PERSON_ENTITY = "person";
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
  public static final List<String> ALL_CONDITION_ATTRIBUTES =
      ImmutableList.of(
          "concept_id",
          "concept_name",
          "vocabulary_id",
          "vocabulary_name",
          "standard_concept",
          "concept_code");

  public static final String PROCEDURE_ENTITY = "procedure";
  public static final List<String> ALL_PROCEDURE_ATTRIBUTES =
      ImmutableList.of(
          "concept_id",
          "concept_name",
          "vocabulary_id",
          "vocabulary_name",
          "standard_concept",
          "concept_code");

  public static final String INGREDIENT_ENTITY = "ingredient";
  public static final List<String> ALL_INGREDIENT_ATTRIBUTES =
      ImmutableList.of(
          "concept_id",
          "concept_name",
          "vocabulary_id",
          "vocabulary_name",
          "standard_concept",
          "concept_code");

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

  public static final String VISIT_ENTITY = "visit";
  public static final List<String> ALL_VISIT_ATTRIBUTES =
      ImmutableList.of("concept_id", "concept_name");

  public static final String OBSERVATION_ENTITY = "observation";
  public static final List<String> ALL_OBSERVATION_ATTRIBUTES =
      ImmutableList.of(
          "concept_id",
          "concept_name",
          "vocabulary_id",
          "vocabulary_name",
          "standard_concept",
          "concept_code");

  public static final String DEVICE_ENTITY = "device";
  public static final List<String> ALL_DEVICE_ATTRIBUTES =
      ImmutableList.of(
          "concept_id",
          "concept_name",
          "vocabulary_id",
          "vocabulary_name",
          "standard_concept",
          "concept_code");
}
