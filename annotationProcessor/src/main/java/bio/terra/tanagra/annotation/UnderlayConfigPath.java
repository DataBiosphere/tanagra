package bio.terra.tanagra.annotation;

import bio.terra.tanagra.underlay.serialization.*;
import java.util.List;

@SuppressWarnings("PMD.CouplingBetweenObjects")
public class UnderlayConfigPath extends AnnotationPath {
  private static final String FILE_TITLE = "Underlay Configuration";
  private static final String FILE_INTRODUCTION =
      "This file lists all the configuration properties available for an underlay, including defining the "
          + "data mapping and the indexing and service deployment data pointers. \n"
          + "This documentation is generated from annotations in the configuration classes.";

  private static final List<Class<?>> CLASSES_TO_WALK =
      List.of(
          SZIndexer.class,
          SZIndexer.Dataflow.class,
          SZService.class,
          SZBigQuery.class,
          SZBigQuery.SourceData.class,
          SZBigQuery.IndexData.class,
          SZUnderlay.class,
          SZUnderlay.Metadata.class,
          SZEntity.class,
          SZEntity.TemporalQuery.class,
          SZAttribute.class,
          SZAttribute.SourceQuery.class,
          SZAttributeSearch.class,
          SZHierarchy.class,
          SZTextSearch.class,
          SZDataType.class,
          SZGroupItems.class,
          SZCriteriaOccurrence.class,
          SZCriteriaOccurrence.OccurrenceEntity.class,
          SZCriteriaOccurrence.OccurrenceEntity.CriteriaRelationship.class,
          SZCriteriaOccurrence.OccurrenceEntity.PrimaryRelationship.class,
          SZCriteriaOccurrence.PrimaryCriteriaRelationship.class,
          SZCriteriaSelector.class,
          SZCriteriaSelector.Display.class,
          SZCriteriaSelector.Modifier.class,
          SZPrepackagedCriteria.class,
          SZRollupCountsSql.class,
          SZVisualization.class,
          SZCorePlugin.class);

  @Override
  public String getTitle() {
    return FILE_TITLE;
  }

  @Override
  public String getIntroduction() {
    return FILE_INTRODUCTION;
  }

  @Override
  public List<Class<?>> getClassesToWalk() {
    return CLASSES_TO_WALK;
  }
}
