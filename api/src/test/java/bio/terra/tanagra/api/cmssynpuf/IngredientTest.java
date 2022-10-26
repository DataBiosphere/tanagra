package bio.terra.tanagra.api.cmssynpuf;

import bio.terra.tanagra.api.BaseQueryTest;
import bio.terra.tanagra.api.EntityQueryRequest;
import bio.terra.tanagra.api.entityfilter.AttributeFilter;
import bio.terra.tanagra.api.entityfilter.HierarchyAncestorFilter;
import bio.terra.tanagra.api.entityfilter.HierarchyParentFilter;
import bio.terra.tanagra.api.entityfilter.HierarchyRootFilter;
import bio.terra.tanagra.api.entityfilter.RelationshipFilter;
import bio.terra.tanagra.api.entityfilter.TextFilter;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.query.filtervariable.BinaryFilterVariable;
import bio.terra.tanagra.query.filtervariable.FunctionFilterVariable;
import bio.terra.tanagra.testing.GeneratedSqlUtils;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.Relationship;
import bio.terra.tanagra.underlay.Underlay;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public class IngredientTest extends BaseQueryTest {
  @Test
  void noFilter() throws IOException {
    EntityQueryRequest entityQueryRequest =
        new EntityQueryRequest.Builder()
            .entity(getEntity())
            .mappingType(Underlay.MappingType.INDEX)
            .selectAttributes(getEntity().getAttributes())
            .limit(DEFAULT_LIMIT)
            .build();
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        querysService.buildInstancesQuery(entityQueryRequest).getSql(),
        "sql/cmssynpuf/ingredient-noFilter.sql");
  }

  @Test
  void textFilter() throws IOException {
    // filter for "ingredient" entity instances that match the search term "alcohol"
    // i.e. ingredients that have a name or synonym that includes "alcohol"
    TextFilter textFilter =
        new TextFilter.Builder()
            .textSearch(getEntity().getTextSearch())
            .functionTemplate(FunctionFilterVariable.FunctionTemplate.TEXT_EXACT_MATCH)
            .text("alcohol")
            .build();

    EntityQueryRequest entityQueryRequest =
        new EntityQueryRequest.Builder()
            .entity(getEntity())
            .mappingType(Underlay.MappingType.INDEX)
            .selectAttributes(getEntity().getAttributes())
            .filter(textFilter)
            .limit(DEFAULT_LIMIT)
            .build();
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        querysService.buildInstancesQuery(entityQueryRequest).getSql(),
        "sql/cmssynpuf/ingredient-textFilter.sql");
  }

  @Test
  void hierarchyRootFilter() throws IOException {
    // filter for "ingredient" entity instances that are root nodes in the "standard" hierarchy
    HierarchyRootFilter hierarchyRootFilter =
        new HierarchyRootFilter(getEntity().getHierarchy("standard"));

    EntityQueryRequest entityQueryRequest =
        new EntityQueryRequest.Builder()
            .entity(getEntity())
            .mappingType(Underlay.MappingType.INDEX)
            .selectAttributes(getEntity().getAttributes())
            .selectHierarchyFields(getEntity().getHierarchy("standard").getFields())
            .filter(hierarchyRootFilter)
            .limit(DEFAULT_LIMIT)
            .build();
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        querysService.buildInstancesQuery(entityQueryRequest).getSql(),
        "sql/cmssynpuf/ingredient-hierarchyRootFilter.sql");
  }

  @Test
  void hierarchyParentFilter() throws IOException {
    // filter for "ingredient" entity instances that are children of the "ingredient" entity
    // instance with concept_id=21603396
    // i.e. give me all the children of "Opium alkaloids and derivatives"
    HierarchyParentFilter hierarchyParentFilter =
        new HierarchyParentFilter(getEntity().getHierarchy("standard"), new Literal(21_603_396L));

    EntityQueryRequest entityQueryRequest =
        new EntityQueryRequest.Builder()
            .entity(getEntity())
            .mappingType(Underlay.MappingType.INDEX)
            .selectAttributes(getEntity().getAttributes())
            .selectHierarchyFields(getEntity().getHierarchy("standard").getFields())
            .filter(hierarchyParentFilter)
            .limit(DEFAULT_LIMIT)
            .build();
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        querysService.buildInstancesQuery(entityQueryRequest).getSql(),
        "sql/cmssynpuf/ingredient-hierarchyParentFilter.sql");
  }

  @Test
  void hierarchyAncestorFilter() throws IOException {
    // filter for "ingredient" entity instances that are descendants of the "ingredient" entity
    // instance with concept_id=21600360
    // i.e. give me all the descendants of "Other cardiac preparations"
    HierarchyAncestorFilter hierarchyAncestorFilter =
        new HierarchyAncestorFilter(getEntity().getHierarchy("standard"), new Literal(21_600_360L));

    EntityQueryRequest entityQueryRequest =
        new EntityQueryRequest.Builder()
            .entity(getEntity())
            .mappingType(Underlay.MappingType.INDEX)
            .selectAttributes(getEntity().getAttributes())
            .selectHierarchyFields(getEntity().getHierarchy("standard").getFields())
            .filter(hierarchyAncestorFilter)
            .limit(DEFAULT_LIMIT)
            .build();
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        querysService.buildInstancesQuery(entityQueryRequest).getSql(),
        "sql/cmssynpuf/ingredient-hierarchyAncestorFilter.sql");
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
        "sql/cmssynpuf/ingredient-relationshipFilter.sql");
  }

  @Override
  protected String getUnderlayName() {
    return "cms_synpuf";
  }

  @Override
  protected String getEntityName() {
    return "ingredient";
  }
}
