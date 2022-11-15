package bio.terra.tanagra.api;

import static bio.terra.tanagra.query.filtervariable.BinaryFilterVariable.BinaryOperator.EQUALS;

import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.query.QueryRequest;
import bio.terra.tanagra.query.filtervariable.BooleanAndOrFilterVariable;
import bio.terra.tanagra.query.filtervariable.FunctionFilterVariable;
import bio.terra.tanagra.service.UnderlaysService;
import bio.terra.tanagra.service.filter.AttributeFilter;
import bio.terra.tanagra.service.filter.BooleanAndOrFilter;
import bio.terra.tanagra.service.filter.EntityFilter;
import bio.terra.tanagra.service.filter.HierarchyAncestorFilter;
import bio.terra.tanagra.service.filter.HierarchyMemberFilter;
import bio.terra.tanagra.service.filter.HierarchyParentFilter;
import bio.terra.tanagra.service.filter.HierarchyRootFilter;
import bio.terra.tanagra.service.filter.RelationshipFilter;
import bio.terra.tanagra.service.filter.TextFilter;
import bio.terra.tanagra.service.instances.EntityQueryRequest;
import bio.terra.tanagra.service.instances.QuerysService;
import bio.terra.tanagra.testing.BaseSpringUnitTest;
import bio.terra.tanagra.testing.GeneratedSqlUtils;
import bio.terra.tanagra.underlay.Attribute;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.EntityGroup;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitygroup.CriteriaOccurrence;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
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

  protected void hierarchyMemberFilter(String hierarchyName) throws IOException {
    HierarchyMemberFilter hierarchyMemberFilter =
        new HierarchyMemberFilter(getEntity().getHierarchy(hierarchyName));

    EntityQueryRequest entityQueryRequest =
        new EntityQueryRequest.Builder()
            .entity(getEntity())
            .mappingType(Underlay.MappingType.INDEX)
            .selectAttributes(getEntity().getAttributes())
            .selectHierarchyFields(getEntity().getHierarchy(hierarchyName).getFields())
            .filter(hierarchyMemberFilter)
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
            + "-hierarchyMemberFilter.sql");
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

  protected void allOccurrencesForSingleCriteriaCohort(
      Entity criteriaEntity, String description, long criteriaEntityId) throws IOException {
    allOccurrencesForSingleCriteriaCohort(
        criteriaEntity,
        description,
        List.of(criteriaEntityId),
        BooleanAndOrFilterVariable.LogicalOperator.OR);
  }

  protected void allOccurrencesForSingleCriteriaCohort(
      Entity criteriaEntity,
      String description,
      List<Long> criteriaEntityIds,
      BooleanAndOrFilterVariable.LogicalOperator logicalOperator)
      throws IOException {
    CriteriaOccurrence criteriaOccurrence = getCriteriaOccurrenceEntityGroup(criteriaEntity);
    EntityFilter cohortFilter =
        buildCohortFilter(criteriaOccurrence, criteriaEntityIds, logicalOperator);

    // Filter for occurrence entity instances that are related to primary entity instances that are
    // related to occurrence entity instances that are related to criteria entity instances that
    // have id=criteriaEntityId.
    // e.g. Occurrences of any condition for people with at least one occurrence of "Type 2 diabetes
    // mellitus". This set of occurrences will include non-diabetes condition occurrences, such as
    // covid.
    RelationshipFilter allOccurrencesForCohort =
        new RelationshipFilter(
            criteriaOccurrence.getOccurrenceEntity(),
            criteriaOccurrence.getOccurrencePrimaryRelationship(),
            cohortFilter);

    EntityQueryRequest entityQueryRequest =
        new EntityQueryRequest.Builder()
            .entity(criteriaOccurrence.getOccurrenceEntity())
            .mappingType(Underlay.MappingType.INDEX)
            .selectAttributes(criteriaOccurrence.getOccurrenceEntity().getAttributes())
            .filter(allOccurrencesForCohort)
            .limit(DEFAULT_LIMIT)
            .build();
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        querysService.buildInstancesQuery(entityQueryRequest).getSql(),
        "sql/"
            + getSqlDirectoryName()
            + "/"
            + criteriaOccurrence.getOccurrenceEntity().getName().replace("_", "")
            + "-"
            + description
            + ".sql");
  }

  protected void singleCriteriaCohort(
      Entity criteriaEntity, String description, long criteriaEntityId) throws IOException {
    singleCriteriaCohort(
        criteriaEntity,
        description,
        List.of(criteriaEntityId),
        BooleanAndOrFilterVariable.LogicalOperator.OR);
  }

  protected void singleCriteriaCohort(
      Entity criteriaEntity,
      String description,
      List<Long> criteriaEntityIds,
      BooleanAndOrFilterVariable.LogicalOperator logicalOperator)
      throws IOException {
    CriteriaOccurrence criteriaOccurrence = getCriteriaOccurrenceEntityGroup(criteriaEntity);
    EntityFilter cohortFilter =
        buildCohortFilter(criteriaOccurrence, criteriaEntityIds, logicalOperator);

    EntityQueryRequest entityQueryRequest =
        new EntityQueryRequest.Builder()
            .entity(criteriaOccurrence.getPrimaryEntity())
            .mappingType(Underlay.MappingType.INDEX)
            .selectAttributes(criteriaOccurrence.getPrimaryEntity().getAttributes())
            .filter(cohortFilter)
            .limit(DEFAULT_LIMIT)
            .build();
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        querysService.buildInstancesQuery(entityQueryRequest).getSql(),
        "sql/"
            + getSqlDirectoryName()
            + "/"
            + criteriaOccurrence.getPrimaryEntity().getName().replace("_", "")
            + "-"
            + description
            + ".sql");
  }

  protected void countSingleCriteriaCohort(
      Entity criteriaEntity,
      String description,
      List<String> groupByAttributes,
      long criteriaEntityId)
      throws IOException {
    countSingleCriteriaCohort(
        criteriaEntity,
        description,
        groupByAttributes,
        List.of(criteriaEntityId),
        BooleanAndOrFilterVariable.LogicalOperator.OR);
  }

  protected void countSingleCriteriaCohort(
      Entity criteriaEntity,
      String description,
      List<String> groupByAttributes,
      List<Long> criteriaEntityIds,
      BooleanAndOrFilterVariable.LogicalOperator logicalOperator)
      throws IOException {
    CriteriaOccurrence criteriaOccurrence = getCriteriaOccurrenceEntityGroup(criteriaEntity);
    EntityFilter cohortFilter =
        buildCohortFilter(criteriaOccurrence, criteriaEntityIds, logicalOperator);
    count(criteriaOccurrence.getPrimaryEntity(), description, groupByAttributes, cohortFilter);
  }

  protected void count(Entity entityToCount, String description, List<String> groupByAttributes)
      throws IOException {
    count(entityToCount, description, groupByAttributes, null);
  }

  private void count(
      Entity entityToCount,
      String description,
      List<String> groupByAttributes,
      @Nullable EntityFilter cohortFilter)
      throws IOException {
    List<Attribute> groupBy =
        groupByAttributes.stream()
            .map(attributeName -> entityToCount.getAttribute(attributeName))
            .collect(Collectors.toList());
    QueryRequest entityCountRequest =
        querysService.buildInstanceCountsQuery(
            entityToCount, Underlay.MappingType.INDEX, groupBy, cohortFilter);
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        entityCountRequest.getSql(),
        "sql/"
            + getSqlDirectoryName()
            + "/"
            + entityToCount.getName().replace("_", "")
            + "-"
            + description
            + "-count.sql");
  }

  /** Lookup the CRITERIA_OCCURRENCE entity group for the given criteria entity. */
  private CriteriaOccurrence getCriteriaOccurrenceEntityGroup(Entity criteriaEntity) {
    // Find the CRITERIA_OCCURRENCE entity group for this entity. We need to lookup the entity group
    // with the criteria entity, not the primary entity, because the primary entity will be a member
    // of many groups.
    Underlay underlay = underlaysService.getUnderlay(getUnderlayName());
    return (CriteriaOccurrence)
        underlay.getEntityGroup(EntityGroup.Type.CRITERIA_OCCURRENCE, criteriaEntity);
  }

  /** Build a RelationshipFilter for a cohort. */
  private EntityFilter buildCohortFilter(
      CriteriaOccurrence criteriaOccurrence,
      List<Long> criteriaEntityIds,
      BooleanAndOrFilterVariable.LogicalOperator logicalOperator) {

    List<EntityFilter> criteriaFilters =
        criteriaEntityIds.stream()
            .map(
                criteriaEntityId -> {
                  // Filter for criteria entity instances that have id=criteriaEntityId.
                  // e.g. Condition "Type 2 diabetes mellitus".
                  AttributeFilter criteria =
                      new AttributeFilter(
                          criteriaOccurrence.getCriteriaEntity().getIdAttribute(),
                          EQUALS,
                          new Literal(criteriaEntityId));

                  // Filter for occurrence entity instances that are related to criteria entity
                  // instances that
                  // have id=criteriaEntityId.
                  // e.g. Occurrences of "Type 2 diabetes mellitus".
                  RelationshipFilter occurrencesOfCriteria =
                      new RelationshipFilter(
                          criteriaOccurrence.getOccurrenceEntity(),
                          criteriaOccurrence.getOccurrenceCriteriaRelationship(),
                          criteria);

                  // Filter for primary entity instances that are related to occurrence entity
                  // instances that are
                  // related to criteria entity instances that have id=criteriaEntityId.
                  // e.g. People with occurrences of "Type 2 diabetes mellitus".
                  return new RelationshipFilter(
                      criteriaOccurrence.getPrimaryEntity(),
                      criteriaOccurrence.getOccurrencePrimaryRelationship(),
                      occurrencesOfCriteria);
                })
            .collect(Collectors.toList());

    if (criteriaFilters.size() == 1) {
      return criteriaFilters.get(0);
    } else {
      return new BooleanAndOrFilter(logicalOperator, criteriaFilters);
    }
  }
}
