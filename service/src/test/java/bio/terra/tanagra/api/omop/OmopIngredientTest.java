package bio.terra.tanagra.api.omop;

import bio.terra.tanagra.api.BaseQueriesTest;
import bio.terra.tanagra.api.query.EntityQueryRequest;
import bio.terra.tanagra.api.query.filter.AttributeFilter;
import bio.terra.tanagra.api.query.filter.RelationshipFilter;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.query.filtervariable.BinaryFilterVariable;
import bio.terra.tanagra.testing.GeneratedSqlUtils;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.Relationship;
import bio.terra.tanagra.underlay.Underlay;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public abstract class OmopIngredientTest extends BaseQueriesTest {
  @Test
  void textFilter() throws IOException {
    // filter for "ingredient" entity instances that match the search term "alcohol"
    // i.e. ingredients that have a name or synonym that includes "alcohol"
    textFilter("alcohol");
  }

  @Test
  void hierarchyRootFilter() throws IOException {
    // filter for "ingredient" entity instances that are root nodes in the "standard" hierarchy
    hierarchyRootFilter("standard");
  }

  @Test
  void hierarchyMemberFilter() throws IOException {
    // filter for "ingredient" entity instances that are members of the "standard" hierarchy
    hierarchyMemberFilter("standard");
  }

  @Test
  void hierarchyParentFilter() throws IOException {
    // filter for "ingredient" entity instances that are children of the "ingredient" entity
    // instance with concept_id=21603396
    // i.e. give me all the children of "Opium alkaloids and derivatives"
    hierarchyParentFilter("standard", 21_603_396L, "opioids");
  }

  @Test
  void hierarchyAncestorFilter() throws IOException {
    // filter for "ingredient" entity instances that are descendants of the "ingredient" entity
    // instance with concept_id=21600360
    // i.e. give me all the descendants of "Other cardiac preparations"
    hierarchyAncestorFilter("standard", 21_600_360L, "cardiacPreparations");
  }

  @Test
  void relationshipFilter() throws IOException {
    Entity brandEntity = underlayService.getEntity(getUnderlayName(), "brand");
    Relationship brandIngredientRelationship = getEntity().getRelationship(brandEntity);

    // filter for "brand" entity instances that have concept_id=19082059
    // i.e. give me the brand "Tylenol Chest Congestion"
    AttributeFilter tylenolChestCongestion =
        new AttributeFilter(
            brandEntity.getAttribute("id"),
            BinaryFilterVariable.BinaryOperator.EQUALS,
            new Literal(19_082_059L));

    // filter for "ingredient" entity instances that are related to "brand" entity instances that
    // have concept_id=19082059
    // i.e. give me all the ingredients in "Tylenol Chest Congestion"
    RelationshipFilter ingredientsInTylenolChestCongestion =
        new RelationshipFilter(
            getEntity(),
            brandIngredientRelationship,
            tylenolChestCongestion,
            /*groupByCountAttribute=*/ null,
            /*groupByCountOperator=*/ null,
            /*groupByCountValue=*/ null);

    EntityQueryRequest entityQueryRequest =
        new EntityQueryRequest.Builder()
            .entity(getEntity())
            .mappingType(Underlay.MappingType.INDEX)
            .selectAttributes(getEntity().getAttributes())
            .filter(ingredientsInTylenolChestCongestion)
            .limit(DEFAULT_LIMIT)
            .build();
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        entityQueryRequest.buildInstancesQuery().getSql(),
        "sql/" + getSqlDirectoryName() + "/ingredient-relationshipFilter.sql");
  }

  @Test
  void cohort() throws IOException {
    // Cohort of people with >=1 occurrence of ingredient = "Ibuprofen".
    singleCriteriaCohort(getEntity(), "ibuprofen", 1_177_480L);
  }

  @Test
  void dataset() throws IOException {
    // Ingredient occurrences for cohort of people with >=1 occurrence of ingredient = "Ibuprofen".
    allOccurrencesForSingleCriteriaCohort(getEntity(), "ibuprofen", 1_177_480L);
  }

  @Override
  protected String getEntityName() {
    return "ingredient";
  }
}