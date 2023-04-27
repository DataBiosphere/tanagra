package bio.terra.tanagra.service.model;

import bio.terra.tanagra.query.Literal;

public class AnnotationValue {
  private final String id;
  private final String keyId;
  private final String entityInstanceId;
  private final Literal literal;

  public AnnotationValue(String id, String keyId, String entityInstanceId, Literal literal) {
    this.id = id;
    this.keyId = keyId;
    this.entityInstanceId = entityInstanceId;
    this.literal = literal;
  }

  public String getId() {
    return id;
  }

  public String getKeyId() {
    return keyId;
  }

  public String getEntityInstanceId() {
    return entityInstanceId;
  }

  public Literal getLiteral() {
    return literal;
  }
}
