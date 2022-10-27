package bio.terra.tanagra.api.omop;

import bio.terra.tanagra.api.BaseQueriesTest;
import bio.terra.tanagra.api.EntityQueryRequest;
import bio.terra.tanagra.api.entityfilter.AttributeFilter;
import bio.terra.tanagra.api.entityfilter.RelationshipFilter;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.query.filtervariable.BinaryFilterVariable;
import bio.terra.tanagra.testing.GeneratedSqlUtils;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.Relationship;
import bio.terra.tanagra.underlay.Underlay;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public abstract class IngredientQueriesTest extends BaseQueriesTest {
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
    Entity brandEntity = underlaysService.getEntity(getUnderlayName(), "brand");
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
        new RelationshipFilter(getEntity(), brandIngredientRelationship, tylenolChestCongestion);

    EntityQueryRequest entityQueryRequest =
        new EntityQueryRequest.Builder()
            .entity(getEntity())
            .mappingType(Underlay.MappingType.INDEX)
            .selectAttributes(getEntity().getAttributes())
            .filter(ingredientsInTylenolChestCongestion)
            .limit(DEFAULT_LIMIT)
            .build();
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        querysService.buildInstancesQuery(entityQueryRequest).getSql(),
        "sql/" + getSqlDirectoryName() + "/ingredient-relationshipFilter.sql");
  }

  @Override
  protected String getEntityName() {
    return "ingredient";
  }
}
