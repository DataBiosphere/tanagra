package bio.terra.tanagra.underlay;

import static bio.terra.tanagra.query.Query.TANAGRA_FIELD_PREFIX;
import static bio.terra.tanagra.underlay.RelationshipMapping.COUNT_FIELD_PREFIX;
import static bio.terra.tanagra.underlay.RelationshipMapping.DISPLAY_HINTS_FIELD_PREFIX;

import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.query.ColumnSchema;

public abstract class RelationshipField {
  public enum Type {
    COUNT,
    DISPLAY_HINTS
  }

  private final Entity entity;
  private Relationship relationship;

  public RelationshipField(Entity entity) {
    this.entity = entity;
  }

  public void initialize(Relationship relationship) {
    this.relationship = relationship;
  }

  public abstract Type getType();

  public abstract ColumnSchema buildColumnSchema();

  public String getFieldAlias(Entity entity) {
    String relatedEntityName =
        relationship.getEntityA().equals(entity)
            ? relationship.getEntityB().getName()
            : relationship.getEntityA().getName();
    return getFieldAlias(relatedEntityName, getType());
  }

  public static String getFieldAlias(String relatedEntityName, Type fieldType) {
    String fieldName;
    switch (fieldType) {
      case COUNT:
        fieldName = COUNT_FIELD_PREFIX;
        break;
      case DISPLAY_HINTS:
        fieldName = DISPLAY_HINTS_FIELD_PREFIX;
        break;
      default:
        throw new SystemException("Unknown relationship field type: " + fieldType);
    }
    return TANAGRA_FIELD_PREFIX + fieldName + relatedEntityName;
  }

  public Relationship getRelationship() {
    return relationship;
  }

  public Entity getEntity() {
    return entity;
  }
}
