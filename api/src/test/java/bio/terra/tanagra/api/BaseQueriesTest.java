package bio.terra.tanagra.api;

import static bio.terra.tanagra.query.filtervariable.BinaryFilterVariable.BinaryOperator.EQUALS;

import bio.terra.tanagra.api.entityfilter.AttributeFilter;
import bio.terra.tanagra.api.entityfilter.HierarchyAncestorFilter;
import bio.terra.tanagra.api.entityfilter.HierarchyParentFilter;
import bio.terra.tanagra.api.entityfilter.HierarchyRootFilter;
import bio.terra.tanagra.api.entityfilter.RelationshipFilter;
import bio.terra.tanagra.api.entityfilter.TextFilter;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.query.filtervariable.FunctionFilterVariable;
import bio.terra.tanagra.testing.BaseSpringUnitTest;
import bio.terra.tanagra.testing.GeneratedSqlUtils;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.EntityGroup;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitygroup.CriteriaOccurrence;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class BaseQueriesTest extends BaseSpringUnitTest {
  protected static final int DEFAULT_LIMIT = 30;

  @Autowired protected UnderlaysService underlaysService;
  @Autowired protected QuerysService querysService;

  private Entity entity;

  @BeforeEach
  void setup() {
    entity = underlaysService.getEntity(getUnderlayName(), getEntityName());
  }

  protected abstract String getUnderlayName();

  protected String getSqlDirectoryName() {
    return getUnderlayName().replace("_", "");
  }

  protected abstract String getEntityName();

  protected Entity getEntity() {
    return entity;
  }

  @Test
  void baseNoFilter() throws IOException {
    EntityQueryRequest entityQueryRequest =
        new EntityQueryRequest.Builder()
            .entity(getEntity())
            .mappingType(Underlay.MappingType.INDEX)
            .selectAttributes(getEntity().getAttributes())
            .limit(DEFAULT_LIMIT)
            .build();
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        querysService.buildInstancesQuery(entityQueryRequest).getSql(),
        "sql/" + getSqlDirectoryName() + "/" + getEntity().getName() + "-noFilter.sql");
  }

  protected void textFilter(String text) throws IOException {
    TextFilter textFilter =
        new TextFilter.Builder()
            .textSearch(getEntity().getTextSearch())
            .functionTemplate(FunctionFilterVariable.FunctionTemplate.TEXT_EXACT_MATCH)
            .text(text)
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
        "sql/" + getSqlDirectoryName() + "/" + getEntity().getName() + "-textFilter.sql");
  }

  protected void hierarchyRootFilter(String hierarchyName) throws IOException {
    HierarchyRootFilter hierarchyRootFilter =
        new HierarchyRootFilter(getEntity().getHierarchy(hierarchyName));

    EntityQueryRequest entityQueryRequest =
        new EntityQueryRequest.Builder()
            .entity(getEntity())
            .mappingType(Underlay.MappingType.INDEX)
            .selectAttributes(getEntity().getAttributes())
            .selectHierarchyFields(getEntity().getHierarchy(hierarchyName).getFields())
            .filter(hierarchyRootFilter)
            .limit(DEFAULT_LIMIT)
            .build();
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        querysService.buildInstancesQuery(entityQueryRequest).getSql(),
        "sql/"
            + getSqlDirectoryName()
            + "/"
            + getEntity().getName()
            + "-"
            + hierarchyName
            + "-hierarchyRootFilter.sql");
  }

  protected void hierarchyParentFilter(String hierarchyName, long parentId, String parentName)
      throws IOException {
    HierarchyParentFilter hierarchyParentFilter =
        new HierarchyParentFilter(getEntity().getHierarchy(hierarchyName), new Literal(parentId));

    EntityQueryRequest entityQueryRequest =
        new EntityQueryRequest.Builder()
            .entity(getEntity())
            .mappingType(Underlay.MappingType.INDEX)
            .selectAttributes(getEntity().getAttributes())
            .selectHierarchyFields(getEntity().getHierarchy(hierarchyName).getFields())
            .filter(hierarchyParentFilter)
            .limit(DEFAULT_LIMIT)
            .build();
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        querysService.buildInstancesQuery(entityQueryRequest).getSql(),
        "sql/"
            + getSqlDirectoryName()
            + "/"
            + getEntity().getName()
            + "-"
            + hierarchyName
            + "-"
            + parentName
            + "-hierarchyParentFilter.sql");
  }

  protected void hierarchyAncestorFilter(String hierarchyName, long ancestorId, String ancestorName)
      throws IOException {
    HierarchyAncestorFilter hierarchyAncestorFilter =
        new HierarchyAncestorFilter(
            getEntity().getHierarchy(hierarchyName), new Literal(ancestorId));

    EntityQueryRequest entityQueryRequest =
        new EntityQueryRequest.Builder()
            .entity(getEntity())
            .mappingType(Underlay.MappingType.INDEX)
            .selectAttributes(getEntity().getAttributes())
            .selectHierarchyFields(getEntity().getHierarchy(hierarchyName).getFields())
            .filter(hierarchyAncestorFilter)
            .limit(DEFAULT_LIMIT)
            .build();
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        querysService.buildInstancesQuery(entityQueryRequest).getSql(),
        "sql/"
            + getSqlDirectoryName()
            + "/"
            + getEntity().getName()
            + "-"
            + hierarchyName
            + "-"
            + ancestorName
            + "-hierarchyAncestorFilter.sql");
  }

  protected void allOccurrencesForPrimariesWithACriteria(long criteriaEntityId, String criteriaName)
      throws IOException {
    // Find the CRITERIA_OCCURRENCE entity group for this entity.
    Underlay underlay = underlaysService.getUnderlay(getUnderlayName());
    EntityGroup entityGroup =
        underlay.getEntityGroup(EntityGroup.Type.CRITERIA_OCCURRENCE, getEntity());
    CriteriaOccurrence criteriaOccurrence = (CriteriaOccurrence) entityGroup;

    // Filter for criteria entity instances that have id=criteriaEntityId.
    // e.g. Condition "Type 2 diabetes mellitus".
    AttributeFilter criteria =
        new AttributeFilter(
            criteriaOccurrence.getCriteriaEntity().getAttribute("id"),
            EQUALS,
            new Literal(criteriaEntityId));

    // Filter for occurrence entity instances that are related to criteria entity instances that
    // have id=criteriaEntityId.
    // e.g. Occurrences of "Type 2 diabetes mellitus".
    RelationshipFilter occurrencesOfCriteria =
        new RelationshipFilter(
            criteriaOccurrence.getOccurrenceEntity(),
            criteriaOccurrence.getOccurrenceCriteriaRelationship(),
            criteria);

    // Filter for primary entity instances that are related to occurrence entity instances that are
    // related to criteria entity instances that have id=criteriaEntityId.
    // e.g. People with occurrences of "Type 2 diabetes mellitus".
    RelationshipFilter primaryWithOccurrencesOfCriteria =
        new RelationshipFilter(
            criteriaOccurrence.getPrimaryEntity(),
            criteriaOccurrence.getOccurrencePrimaryRelationship(),
            occurrencesOfCriteria);

    // Filter for occurrence entity instances that are related to primary entity instances that are
    // related to occurrence entity instances that are related to criteria entity instances that
    // have id=criteriaEntityId.
    // e.g. Occurrences of any condition for people with at least one occurrence of "Type 2 diabetes
    // mellitus". This set of occurrences will include non-diabetes condition occurrences, such as
    // covid.
    RelationshipFilter allOccurrencesForPrimaryWithOccurrencesOfCriteria =
        new RelationshipFilter(
            criteriaOccurrence.getOccurrenceEntity(),
            criteriaOccurrence.getOccurrencePrimaryRelationship(),
            primaryWithOccurrencesOfCriteria);

    EntityQueryRequest entityQueryRequest =
        new EntityQueryRequest.Builder()
            .entity(criteriaOccurrence.getOccurrenceEntity())
            .mappingType(Underlay.MappingType.INDEX)
            .selectAttributes(criteriaOccurrence.getOccurrenceEntity().getAttributes())
            .filter(allOccurrencesForPrimaryWithOccurrencesOfCriteria)
            .limit(DEFAULT_LIMIT)
            .build();
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        querysService.buildInstancesQuery(entityQueryRequest).getSql(),
        "sql/"
            + getSqlDirectoryName()
            + "/"
            + criteriaOccurrence.getOccurrenceEntity().getName().replace("_", "")
            + "-"
            + criteriaName
            + ".sql");
  }
}
