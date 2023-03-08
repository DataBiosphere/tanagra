package bio.terra.tanagra.api;

import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.query.QueryRequest;
import bio.terra.tanagra.query.filtervariable.BinaryFilterVariable;
import bio.terra.tanagra.query.filtervariable.BinaryFilterVariable.BinaryOperator;
import bio.terra.tanagra.query.filtervariable.BooleanAndOrFilterVariable;
import bio.terra.tanagra.query.filtervariable.FunctionFilterVariable;
import bio.terra.tanagra.service.QuerysService;
import bio.terra.tanagra.service.UnderlaysService;
import bio.terra.tanagra.service.instances.EntityQueryRequest;
import bio.terra.tanagra.service.instances.filter.AttributeFilter;
import bio.terra.tanagra.service.instances.filter.BooleanAndOrFilter;
import bio.terra.tanagra.service.instances.filter.EntityFilter;
import bio.terra.tanagra.service.instances.filter.HierarchyAncestorFilter;
import bio.terra.tanagra.service.instances.filter.HierarchyMemberFilter;
import bio.terra.tanagra.service.instances.filter.HierarchyParentFilter;
import bio.terra.tanagra.service.instances.filter.HierarchyRootFilter;
import bio.terra.tanagra.service.instances.filter.RelationshipFilter;
import bio.terra.tanagra.service.instances.filter.TextFilter;
import bio.terra.tanagra.testing.BaseSpringUnitTest;
import bio.terra.tanagra.testing.GeneratedSqlUtils;
import bio.terra.tanagra.underlay.Attribute;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.EntityGroup;
import bio.terra.tanagra.underlay.Relationship;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitygroup.CriteriaOccurrence;
import bio.terra.tanagra.underlay.entitygroup.GroupItems;
import com.google.common.collect.ImmutableList;
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
    textFilter(/*attributeName=*/ null, text);
  }

  protected void textFilter(@Nullable String attributeName, String text) throws IOException {
    TextFilter.Builder textFilterBuilder =
        new TextFilter.Builder()
            .textSearch(getEntity().getTextSearch())
            .functionTemplate(FunctionFilterVariable.FunctionTemplate.TEXT_EXACT_MATCH)
            .text(text);
    if (attributeName != null) {
      textFilterBuilder.attribute(querysService.getAttribute(entity, "id"));
    }
    TextFilter textFilter = textFilterBuilder.build();

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
        buildCohortFilterOccurrence(
            criteriaOccurrence,
            criteriaEntityIds,
            logicalOperator,
            /*groupByCountAttribute=*/ null,
            /*groupByCountOperator=*/ null,
            /*groupByCountValue=*/ null);

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
            cohortFilter,
            /*groupByCountAttribute=*/ null,
            /*groupByCountOperator=*/ null,
            /*groupByCountValue=*/ null);

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
        BooleanAndOrFilterVariable.LogicalOperator.OR,
        /*groupByCountAttribute=*/ null,
        /*groupByCountOperator=*/ null,
        /*groupByCountValue=*/ null);
  }

  protected void singleCriteriaCohort(
      Entity criteriaEntity,
      String description,
      List<Long> criteriaEntityIds,
      BooleanAndOrFilterVariable.LogicalOperator logicalOperator,
      @Nullable Attribute groupByCountAttribute,
      @Nullable BinaryFilterVariable.BinaryOperator groupByCountOperator,
      @Nullable Integer groupByCountValue)
      throws IOException {
    CriteriaOccurrence criteriaOccurrence = getCriteriaOccurrenceEntityGroup(criteriaEntity);
    EntityFilter cohortFilter =
        buildCohortFilterOccurrence(
            criteriaOccurrence,
            criteriaEntityIds,
            logicalOperator,
            groupByCountAttribute,
            groupByCountOperator,
            groupByCountValue);

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

  // Filter for when there's no occurrence entity. This has fewer levels of nesting than
  // singleCriteriaCohort().
  protected void relationshipCohort(String filterAttributeName, String text) throws IOException {
    // Build select attribute: person id
    Underlay underlay = underlaysService.getUnderlay(getUnderlayName());
    String selectEntityName = "person";
    Entity selectEntity = underlay.getEntity(selectEntityName);
    Attribute selectAttribute = entity.getAttribute("id");

    EntityFilter cohortFilter =
        buildCohortFilterNoOccurrence(selectEntity, filterAttributeName, text);

    EntityQueryRequest entityQueryRequest =
        new EntityQueryRequest.Builder()
            .entity(selectEntity)
            .mappingType(Underlay.MappingType.INDEX)
            .selectAttributes(ImmutableList.of(selectAttribute))
            .filter(cohortFilter)
            .limit(DEFAULT_LIMIT)
            .build();
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        querysService.buildInstancesQuery(entityQueryRequest).getSql(),
        "sql/"
            + getSqlDirectoryName()
            + "/"
            + selectEntityName
            + "-"
            + getEntity().getName()
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
        buildCohortFilterOccurrence(
            criteriaOccurrence,
            criteriaEntityIds,
            logicalOperator,
            /*groupByCountAttribute=*/ null,
            /*groupByCountOperator=*/ null,
            /*groupByCountValue=*/ null);
    count(criteriaOccurrence.getPrimaryEntity(), description, groupByAttributes, cohortFilter);
  }

  // Count for when there's no occurrence entity. This has fewer levels of nesting than
  // countSingleCriteriaCohort().
  protected void countRelationshipCohort(
      List<String> groupByAttributes, String filterAttributeName, String text) throws IOException {
    Underlay underlay = underlaysService.getUnderlay(getUnderlayName());
    Entity countEntity = underlay.getEntity("person");
    EntityFilter cohortFilter =
        buildCohortFilterNoOccurrence(countEntity, filterAttributeName, text);

    count(countEntity, /*description=*/ getEntityName(), groupByAttributes, cohortFilter);
  }

  protected void count(Entity countEntity, String description, List<String> groupByAttributes)
      throws IOException {
    count(countEntity, description, groupByAttributes, null);
  }

  private void count(
      Entity countEntity,
      String description,
      List<String> groupByAttributes,
      @Nullable EntityFilter cohortFilter)
      throws IOException {
    List<Attribute> groupBy =
        groupByAttributes.stream()
            .map(attributeName -> countEntity.getAttribute(attributeName))
            .collect(Collectors.toList());
    QueryRequest entityCountRequest =
        querysService.buildInstanceCountsQuery(
            countEntity, Underlay.MappingType.INDEX, groupBy, cohortFilter);
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        entityCountRequest.getSql(),
        "sql/"
            + getSqlDirectoryName()
            + "/"
            + countEntity.getName().replace("_", "")
            + "-"
            + description
            + "-count.sql");
  }

  /** Lookup the CRITERIA_OCCURRENCE entity group for the given criteria entity. */
  protected CriteriaOccurrence getCriteriaOccurrenceEntityGroup(Entity criteriaEntity) {
    // Find the CRITERIA_OCCURRENCE entity group for this entity. We need to lookup the entity group
    // with the criteria entity, not the primary entity, because the primary entity will be a member
    // of many groups.
    Underlay underlay = underlaysService.getUnderlay(getUnderlayName());
    return (CriteriaOccurrence)
        underlay.getEntityGroup(EntityGroup.Type.CRITERIA_OCCURRENCE, criteriaEntity);
  }

  private GroupItems getGroupItemsEntityGroup(Entity groupEntity) {
    Underlay underlay = underlaysService.getUnderlay(getUnderlayName());
    return (GroupItems) underlay.getEntityGroup(EntityGroup.Type.GROUP_ITEMS, groupEntity);
  }

  /** Build a RelationshipFilter for a cohort when there's an occurrence entity. */
  private EntityFilter buildCohortFilterOccurrence(
      CriteriaOccurrence criteriaOccurrence,
      List<Long> criteriaEntityIds,
      BooleanAndOrFilterVariable.LogicalOperator logicalOperator,
      @Nullable Attribute groupByCountAttribute,
      @Nullable BinaryFilterVariable.BinaryOperator groupByCountOperator,
      @Nullable Integer groupByCountValue) {

    List<EntityFilter> criteriaFilters =
        criteriaEntityIds.stream()
            .map(
                criteriaEntityId -> {
                  // Filter for criteria entity instances that have id=criteriaEntityId.
                  // e.g. Condition "Type 2 diabetes mellitus".
                  AttributeFilter criteria =
                      new AttributeFilter(
                          criteriaOccurrence.getCriteriaEntity().getIdAttribute(),
                          BinaryOperator.EQUALS,
                          new Literal(criteriaEntityId));

                  // Filter for occurrence entity instances that are related to criteria entity
                  // instances that
                  // have id=criteriaEntityId.
                  // e.g. Occurrences of "Type 2 diabetes mellitus".
                  RelationshipFilter occurrencesOfCriteria =
                      new RelationshipFilter(
                          criteriaOccurrence.getOccurrenceEntity(),
                          criteriaOccurrence.getOccurrenceCriteriaRelationship(),
                          criteria,
                          /*groupByCountAttribute=*/ null,
                          /*groupByCountOperator=*/ null,
                          /*groupByCountValue=*/ null);

                  // Filter for primary entity instances that are related to occurrence entity
                  // instances that are
                  // related to criteria entity instances that have id=criteriaEntityId.
                  // e.g. People with occurrences of "Type 2 diabetes mellitus".
                  return new RelationshipFilter(
                      criteriaOccurrence.getPrimaryEntity(),
                      criteriaOccurrence.getOccurrencePrimaryRelationship(),
                      occurrencesOfCriteria,
                      groupByCountAttribute,
                      groupByCountOperator,
                      groupByCountValue);
                })
            .collect(Collectors.toList());

    if (criteriaFilters.size() == 1) {
      return criteriaFilters.get(0);
    } else {
      return new BooleanAndOrFilter(logicalOperator, criteriaFilters);
    }
  }

  /**
   * Build a RelationshipFilter for a cohort when there's no occurrence entity. There are fewer
   * levels of nesting than in buildCohortFilterOccurrence().
   */
  private EntityFilter buildCohortFilterNoOccurrence(
      Entity selectEntity, String filterAttributeName, String text) {
    Relationship relationship =
        getGroupItemsEntityGroup(entity).getRelationship(entity, selectEntity).orElseThrow();
    EntityFilter subfilter =
        new AttributeFilter(
            entity.getAttribute(filterAttributeName), BinaryOperator.EQUALS, new Literal(text));
    return new RelationshipFilter(
        selectEntity,
        relationship,
        subfilter,
        /*groupByCountAttribute=*/ null,
        /*groupByCountOperator=*/ null,
        /*groupByCountValue=*/ null);
  }
}
