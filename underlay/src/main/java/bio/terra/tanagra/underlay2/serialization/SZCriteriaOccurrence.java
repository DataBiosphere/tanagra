package bio.terra.tanagra.underlay2.serialization;

import java.util.Set;

public class SZCriteriaOccurrence {
  public String name;
  public String criteriaEntity;
  public Set<OccurrenceEntity> occurrenceEntities;
  public PrimaryCriteriaRelationship primaryCriteriaRelationship;

  public static class OccurrenceEntity {
    public String occurrenceEntity;
    public CriteriaRelationship criteriaRelationship;
    public PrimaryRelationship primaryRelationship;
    public Set<String> attributesWithInstanceLevelHints;

    public static class CriteriaRelationship {
      public String foreignKeyAttributeOccurrenceEntity;
      public String idPairsSqlFile;
      public String occurrenceEntityIdFieldName;
      public String criteriaEntityIdFieldName;
    }

    public static class PrimaryRelationship {
      public String foreignKeyAttributeOccurrenceEntity;
      public String idPairsSqlFile;
      public String occurrenceEntityIdFieldName;
      public String primaryEntityIdFieldName;
    }
  }

  public static class PrimaryCriteriaRelationship {
    public String idPairsSqlFile;
    public String primaryEntityIdFieldName;
    public String criteriaEntityIdFieldName;
  }
}
