package bio.terra.tanagra.underlay.serialization;

import bio.terra.tanagra.annotation.AnnotatedClass;
import bio.terra.tanagra.annotation.AnnotatedField;

@AnnotatedClass(
    name = "SZRollupCountsSql",
    markdown =
        "Pointer to SQL that returns entity id - rollup count (= number of related entity instances) pairs "
            + "(e.g. variant - number of people). Useful when there's an easy way to calculate these in SQL "
            + "and we want to avoid ingesting the full entity - related entity relationship id pairs table into Dataflow.")
public class SZRollupCountsSql {
  @AnnotatedField(
      name = "SZRollupCountsSql.rollupCountsSqlFile",
      markdown =
          "Name of the entity id - rollup counts (= number of items entity instances) pairs SQL file.\n\n"
              + "File must be in the same directory as the entity/group file. Name includes file extension.\n\n"
              + "There can be other columns selected in the SQL file (e.g. `SELECT * FROM relationships`), but the "
              + "entity id and rollup count fields are required.",
      exampleValue = "rollupCounts.sql")
  public String sqlFile;

  @AnnotatedField(
      name = "SZRollupCountsSql.entityIdFieldName",
      markdown = "Name of the field or column name that maps to the entity id.",
      exampleValue = "entity_id")
  public String entityIdFieldName;

  @AnnotatedField(
      name = "SZRollupCountsSql.rollupCountFieldName",
      markdown = "Name of the field or column name that maps to the rollup count per entity id.",
      exampleValue = "rollup_count")
  public String rollupCountFieldName;
}
