package bio.terra.tanagra.service.model;

import bio.terra.tanagra.query.Literal;
import java.util.List;
import javax.annotation.Nullable;

public class AnnotationKey {
  private final String id;
  private final String displayName;
  private final @Nullable String description;
  private final Literal.DataType dataType;
  private final List<String> enumVals;

  public AnnotationKey(
      String id,
      String displayName,
      @Nullable String description,
      Literal.DataType dataType,
      List<String> enumVals) {
    this.id = id;
    this.displayName = displayName;
    this.description = description;
    this.dataType = dataType;
    this.enumVals = enumVals;
  }

  public String getId() {
    return id;
  }

  public String getDisplayName() {
    return displayName;
  }

  @Nullable
  public String getDescription() {
    return description;
  }

  public Literal.DataType getDataType() {
    return dataType;
  }

  public List<String> getEnumVals() {
    return enumVals;
  }
}
