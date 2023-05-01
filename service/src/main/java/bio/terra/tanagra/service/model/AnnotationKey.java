package bio.terra.tanagra.service.model;

import bio.terra.tanagra.query.Literal;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.commons.lang3.RandomStringUtils;

public class AnnotationKey {
  private final String id;
  private final String displayName;
  private final String description;
  private final Literal.DataType dataType;
  private final List<String> enumVals;

  public AnnotationKey(
      String id,
      String displayName,
      String description,
      Literal.DataType dataType,
      List<String> enumVals) {
    this.id = id;
    this.displayName = displayName;
    this.description = description;
    this.dataType = dataType;
    this.enumVals = enumVals;
  }

  public static Builder builder() {
    return new Builder();
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

  public static class Builder {
    private String id;
    private String displayName;
    private String description;
    private Literal.DataType dataType;
    private List<String> enumVals = new ArrayList<>();

    public Builder id(String id) {
      this.id = id;
      return this;
    }

    public Builder displayName(String displayName) {
      this.displayName = displayName;
      return this;
    }

    public Builder description(String description) {
      this.description = description;
      return this;
    }

    public Builder dataType(Literal.DataType dataType) {
      this.dataType = dataType;
      return this;
    }

    public Builder enumVals(List<String> enumVals) {
      this.enumVals = enumVals;
      return this;
    }

    public AnnotationKey build() {
      if (id == null) {
        id = RandomStringUtils.randomAlphanumeric(10);
      }
      return new AnnotationKey(id, displayName, description, dataType, enumVals);
    }

    public String getId() {
      return id;
    }
  }
}
