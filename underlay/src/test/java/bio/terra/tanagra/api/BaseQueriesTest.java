package bio.terra.tanagra.api;

import bio.terra.common.exception.NotFoundException;
import bio.terra.tanagra.api.field.AttributeField;
import bio.terra.tanagra.api.field.HierarchyIsMemberField;
import bio.terra.tanagra.api.field.HierarchyIsRootField;
import bio.terra.tanagra.api.field.HierarchyNumChildrenField;
import bio.terra.tanagra.api.field.HierarchyPathField;
import bio.terra.tanagra.api.field.ValueDisplayField;
import bio.terra.tanagra.api.filter.AttributeFilter;
import bio.terra.tanagra.api.filter.BooleanAndOrFilter;
import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.api.filter.HierarchyHasAncestorFilter;
import bio.terra.tanagra.api.filter.HierarchyHasParentFilter;
import bio.terra.tanagra.api.filter.HierarchyIsMemberFilter;
import bio.terra.tanagra.api.filter.HierarchyIsRootFilter;
import bio.terra.tanagra.api.filter.RelationshipFilter;
import bio.terra.tanagra.api.filter.TextSearchFilter;
import bio.terra.tanagra.api.query.EntityQueryRunner;
import bio.terra.tanagra.api.query.count.CountQueryRequest;
import bio.terra.tanagra.api.query.list.ListQueryRequest;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.query.filtervariable.BinaryFilterVariable;
import bio.terra.tanagra.query.filtervariable.BinaryFilterVariable.BinaryOperator;
import bio.terra.tanagra.query.filtervariable.BooleanAndOrFilterVariable;
import bio.terra.tanagra.query.filtervariable.FunctionFilterVariable;
import bio.terra.tanagra.testing.GeneratedSqlUtils;
import bio.terra.tanagra.underlay.ConfigReader;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.Hierarchy;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.CriteriaOccurrence;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.GroupItems;
import bio.terra.tanagra.underlay.serialization.SZService;
import bio.terra.tanagra.underlay.serialization.SZUnderlay;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public abstract class BaseQueriesTest {
  protected static final int DEFAULT_LIMIT = 30;
  private Underlay underlay;
  private Entity entity;

  @BeforeEach
  void setup() {
    SZService szService = ConfigReader.deserializeService(getServiceConfigName());
    SZUnderlay szUnderlay = ConfigReader.deserializeUnderlay(szService.underlay);
    underlay = Underlay.fromConfig(szService.bigQuery, szUnderlay);
    entity = underlay.getEntity(getEntityName());
  }

  protected abstract String getServiceConfigName();

  protected String getSqlDirectoryName() {
    return underlay.getName().replace("_", "");
  }

  protected abstract String getEntityName();

  protected Underlay getUnderlay() {
    return underlay;
  }

  protected Entity getEntity() {
    return entity;
  }

  @Test
  void baseNoFilter() throws IOException {
    // Select all attributes.
    List<ValueDisplayField> selectFields =
        entity.getAttributes().stream()
            .map(attribute -> new AttributeField(underlay, entity, attribute, false, false))
            .collect(Collectors.toList());
    ListQueryRequest listQueryRequest =
        new ListQueryRequest(underlay, entity, selectFields, null, null, DEFAULT_LIMIT, null, null);
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        EntityQueryRunner.buildQueryRequest(listQueryRequest).getSql(),
        "sql/" + getSqlDirectoryName() + "/" + getEntity().getName() + "-noFilter.sql");
  }

  protected void textFilter(String text) throws IOException {
    textFilter(/*attributeName=*/ null, text);
  }

  protected void textFilter(@Nullable String attributeName, String text) throws IOException {
    TextSearchFilter textSearchFilter;
    if (attributeName == null) {
      textSearchFilter =
          new TextSearchFilter(
              underlay,
              entity,
              FunctionFilterVariable.FunctionTemplate.TEXT_EXACT_MATCH,
              text,
              null);
    } else {
      textSearchFilter =
          new TextSearchFilter(
              underlay,
              entity,
              FunctionFilterVariable.FunctionTemplate.TEXT_EXACT_MATCH,
              text,
              entity.getAttribute(attributeName));
    }

    // Select all attributes.
    List<ValueDisplayField> selectFields =
        entity.getAttributes().stream()
            .map(attribute -> new AttributeField(underlay, entity, attribute, false, false))
            .collect(Collectors.toList());
    ListQueryRequest listQueryRequest =
        new ListQueryRequest(
            underlay, entity, selectFields, textSearchFilter, null, DEFAULT_LIMIT, null, null);
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        EntityQueryRunner.buildQueryRequest(listQueryRequest).getSql(),
        "sql/" + getSqlDirectoryName() + "/" + getEntity().getName() + "-textFilter.sql");
  }

  protected void hierarchyRootFilter(String hierarchyName) throws IOException {
    Hierarchy hierarchy = entity.getHierarchy(hierarchyName);

    // Select all attributes and hierarchy fields.
    List<ValueDisplayField> selectFields = new ArrayList<>();
    entity.getAttributes().stream()
        .forEach(
            attribute ->
                selectFields.add(new AttributeField(underlay, entity, attribute, false, false)));
    selectFields.add(new HierarchyPathField(underlay, entity, hierarchy));
    selectFields.add(new HierarchyNumChildrenField(underlay, entity, hierarchy));
    selectFields.add(new HierarchyIsRootField(underlay, entity, hierarchy));
    selectFields.add(new HierarchyIsMemberField(underlay, entity, hierarchy));

    HierarchyIsRootFilter hierarchyIsRootFilter =
        new HierarchyIsRootFilter(underlay, entity, hierarchy);
    ListQueryRequest listQueryRequest =
        new ListQueryRequest(
            underlay, entity, selectFields, hierarchyIsRootFilter, null, DEFAULT_LIMIT, null, null);
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        EntityQueryRunner.buildQueryRequest(listQueryRequest).getSql(),
        "sql/"
            + getSqlDirectoryName()
            + "/"
            + getEntity().getName()
            + "-"
            + hierarchyName
            + "-hierarchyRootFilter.sql");
  }

  protected void hierarchyMemberFilter(String hierarchyName) throws IOException {
    Hierarchy hierarchy = entity.getHierarchy(hierarchyName);

    // Select all attributes and hierarchy fields.
    List<ValueDisplayField> selectFields = new ArrayList<>();
    entity.getAttributes().stream()
        .forEach(
            attribute ->
                selectFields.add(new AttributeField(underlay, entity, attribute, false, false)));
    selectFields.add(new HierarchyPathField(underlay, entity, hierarchy));
    selectFields.add(new HierarchyNumChildrenField(underlay, entity, hierarchy));
    selectFields.add(new HierarchyIsRootField(underlay, entity, hierarchy));
    selectFields.add(new HierarchyIsMemberField(underlay, entity, hierarchy));

    HierarchyIsMemberFilter hierarchyIsMemberFilter =
        new HierarchyIsMemberFilter(underlay, entity, hierarchy);
    ListQueryRequest listQueryRequest =
        new ListQueryRequest(
            underlay,
            entity,
            selectFields,
            hierarchyIsMemberFilter,
            null,
            DEFAULT_LIMIT,
            null,
            null);
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        EntityQueryRunner.buildQueryRequest(listQueryRequest).getSql(),
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
    Hierarchy hierarchy = entity.getHierarchy(hierarchyName);

    // Select all attributes and hierarchy fields.
    List<ValueDisplayField> selectFields = new ArrayList<>();
    entity.getAttributes().stream()
        .forEach(
            attribute ->
                selectFields.add(new AttributeField(underlay, entity, attribute, false, false)));
    selectFields.add(new HierarchyPathField(underlay, entity, hierarchy));
    selectFields.add(new HierarchyNumChildrenField(underlay, entity, hierarchy));
    selectFields.add(new HierarchyIsRootField(underlay, entity, hierarchy));
    selectFields.add(new HierarchyIsMemberField(underlay, entity, hierarchy));

    HierarchyHasParentFilter hierarchyHasParentFilter =
        new HierarchyHasParentFilter(underlay, entity, hierarchy, new Literal(parentId));
    ListQueryRequest listQueryRequest =
        new ListQueryRequest(
            underlay,
            entity,
            selectFields,
            hierarchyHasParentFilter,
            null,
            DEFAULT_LIMIT,
            null,
            null);
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        EntityQueryRunner.buildQueryRequest(listQueryRequest).getSql(),
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
    Hierarchy hierarchy = entity.getHierarchy(hierarchyName);

    // Select all attributes and hierarchy fields.
    List<ValueDisplayField> selectFields = new ArrayList<>();
    entity.getAttributes().stream()
        .forEach(
            attribute ->
                selectFields.add(new AttributeField(underlay, entity, attribute, false, false)));
    selectFields.add(new HierarchyPathField(underlay, entity, hierarchy));
    selectFields.add(new HierarchyNumChildrenField(underlay, entity, hierarchy));
    selectFields.add(new HierarchyIsRootField(underlay, entity, hierarchy));
    selectFields.add(new HierarchyIsMemberField(underlay, entity, hierarchy));

    HierarchyHasAncestorFilter hierarchyHasAncestorFilter =
        new HierarchyHasAncestorFilter(underlay, entity, hierarchy, new Literal(ancestorId));
    ListQueryRequest listQueryRequest =
        new ListQueryRequest(
            underlay,
            entity,
            selectFields,
            hierarchyHasAncestorFilter,
            null,
            DEFAULT_LIMIT,
            null,
            null);
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        EntityQueryRunner.buildQueryRequest(listQueryRequest).getSql(),
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
    Entity occurrenceEntity = criteriaOccurrence.getOccurrenceEntities().get(0);
    RelationshipFilter allOccurrencesForCohort =
        new RelationshipFilter(
            underlay,
            criteriaOccurrence,
            occurrenceEntity,
            criteriaOccurrence.getOccurrencePrimaryRelationship(occurrenceEntity.getName()),
            cohortFilter,
            /*groupByCountAttribute=*/ null,
            /*groupByCountOperator=*/ null,
            /*groupByCountValue=*/ null);

    // Select all attributes.
    List<ValueDisplayField> selectFields =
        occurrenceEntity.getAttributes().stream()
            .map(
                attribute ->
                    new AttributeField(underlay, occurrenceEntity, attribute, false, false))
            .collect(Collectors.toList());
    ListQueryRequest listQueryRequest =
        new ListQueryRequest(
            underlay,
            occurrenceEntity,
            selectFields,
            allOccurrencesForCohort,
            null,
            DEFAULT_LIMIT,
            null,
            null);
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        EntityQueryRunner.buildQueryRequest(listQueryRequest).getSql(),
        "sql/"
            + getSqlDirectoryName()
            + "/"
            + occurrenceEntity.getName().replace("_", "")
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

    // Select all attributes.
    List<ValueDisplayField> selectFields =
        criteriaOccurrence.getPrimaryEntity().getAttributes().stream()
            .map(
                attribute ->
                    new AttributeField(
                        underlay, criteriaOccurrence.getPrimaryEntity(), attribute, false, false))
            .collect(Collectors.toList());
    ListQueryRequest listQueryRequest =
        new ListQueryRequest(
            underlay,
            criteriaOccurrence.getPrimaryEntity(),
            selectFields,
            cohortFilter,
            null,
            DEFAULT_LIMIT,
            null,
            null);
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        EntityQueryRunner.buildQueryRequest(listQueryRequest).getSql(),
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
    String selectEntityName = "person";
    Entity selectEntity = underlay.getEntity(selectEntityName);

    EntityFilter cohortFilter =
        buildCohortFilterNoOccurrence(selectEntity, filterAttributeName, text);

    // Select all attributes.
    List<ValueDisplayField> selectFields =
        List.of(new AttributeField(underlay, selectEntity, entity.getIdAttribute(), false, false));
    ListQueryRequest listQueryRequest =
        new ListQueryRequest(
            underlay, selectEntity, selectFields, cohortFilter, null, DEFAULT_LIMIT, null, null);
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        EntityQueryRunner.buildQueryRequest(listQueryRequest).getSql(),
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
    List<ValueDisplayField> groupByFields =
        groupByAttributes.stream()
            .map(
                groupByAttributeName ->
                    new AttributeField(
                        underlay,
                        countEntity,
                        countEntity.getAttribute(groupByAttributeName),
                        true,
                        false))
            .collect(Collectors.toList());
    CountQueryRequest countQueryRequest =
        new CountQueryRequest(underlay, countEntity, groupByFields, cohortFilter, null, null, null);
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        EntityQueryRunner.buildQueryRequest(countQueryRequest).getSql(),
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
    return (CriteriaOccurrence)
        underlay.getEntityGroups().stream()
            .filter(
                eg ->
                    (eg instanceof CriteriaOccurrence)
                        && ((CriteriaOccurrence) eg).getCriteriaEntity().equals(criteriaEntity))
            .findAny()
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "CriteriaOccurrence entity group not found for criteriaEntity="
                            + criteriaEntity.getName()));
  }

  private GroupItems getGroupItemsEntityGroup(Entity groupEntity) {
    return (GroupItems)
        underlay.getEntityGroups().stream()
            .filter(
                eg ->
                    (eg instanceof GroupItems)
                        && ((GroupItems) eg).getGroupEntity().equals(groupEntity))
            .findAny()
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "GroupItems entity group not found for groupEntity="
                            + groupEntity.getName()));
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
                          underlay,
                          criteriaOccurrence.getCriteriaEntity(),
                          criteriaOccurrence.getCriteriaEntity().getIdAttribute(),
                          BinaryOperator.EQUALS,
                          new Literal(criteriaEntityId));

                  // Filter for occurrence entity instances that are related to criteria entity
                  // instances that
                  // have id=criteriaEntityId.
                  // e.g. Occurrences of "Type 2 diabetes mellitus".
                  Entity occurrenceEntity = criteriaOccurrence.getOccurrenceEntities().get(0);
                  RelationshipFilter occurrencesOfCriteria =
                      new RelationshipFilter(
                          underlay,
                          criteriaOccurrence,
                          occurrenceEntity,
                          criteriaOccurrence.getOccurrenceCriteriaRelationship(
                              occurrenceEntity.getName()),
                          criteria,
                          /*groupByCountAttribute=*/ null,
                          /*groupByCountOperator=*/ null,
                          /*groupByCountValue=*/ null);

                  // Filter for primary entity instances that are related to occurrence entity
                  // instances that are
                  // related to criteria entity instances that have id=criteriaEntityId.
                  // e.g. People with occurrences of "Type 2 diabetes mellitus".
                  return new RelationshipFilter(
                      underlay,
                      criteriaOccurrence,
                      criteriaOccurrence.getPrimaryEntity(),
                      criteriaOccurrence.getOccurrencePrimaryRelationship(
                          occurrenceEntity.getName()),
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
    GroupItems groupItems = getGroupItemsEntityGroup(entity);
    EntityFilter subfilter =
        new AttributeFilter(
            underlay,
            entity,
            entity.getAttribute(filterAttributeName),
            BinaryOperator.EQUALS,
            new Literal(text));
    return new RelationshipFilter(
        underlay,
        groupItems,
        selectEntity,
        groupItems.getGroupItemsRelationship(),
        subfilter,
        /*groupByCountAttribute=*/ null,
        /*groupByCountOperator=*/ null,
        /*groupByCountValue=*/ null);
  }
}
