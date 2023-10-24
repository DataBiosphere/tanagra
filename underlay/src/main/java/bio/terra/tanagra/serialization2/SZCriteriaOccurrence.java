package bio.terra.tanagra.serialization2;

import java.util.Set;

public class SZCriteriaOccurrence {
  public String name;
  public String criteriaEntity;
  public Set<OccurrenceCriteriaRelationship> occurrenceCriteriaRelationships;
  public Set<OccurrencePrimaryRelationship> occurrencePrimaryRelationships;
  public PrimaryCriteriaRelationship primaryCriteriaRelationship;

  public static class OccurrenceCriteriaRelationship {
    public String occurrenceEntity;
    public String foreignKeyAttributeOccurrenceEntity;
    public String idPairsSqlFile;
    public String occurrenceEntityIdFieldName;
    public String criteriaEntityIdFieldName;
  }

  public static class OccurrencePrimaryRelationship {
    public String occurrenceEntity;
    public String foreignKeyAttributeOccurrenceEntity;
    public String idPairsSqlFile;
    public String occurrenceEntityIdFieldName;
    public String primaryEntityIdFieldName;
  }

  public static class PrimaryCriteriaRelationship {
    public String foreignKeyAttributePrimaryEntity;
    public String idPairsSqlFile;
    public String primaryEntityIdFieldName;
    public String criteriaEntityIdFieldName;
  }
}
