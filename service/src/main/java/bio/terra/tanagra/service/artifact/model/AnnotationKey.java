package bio.terra.tanagra.service.artifact.model;

import static bio.terra.tanagra.service.artifact.model.Study.MAX_DISPLAY_NAME_LENGTH;

import bio.terra.common.exception.BadRequestException;
import bio.terra.tanagra.api.shared.DataType;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.exception.SystemException;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.RandomStringUtils;

public final class AnnotationKey {
  private final String id;
  private final String displayName;
  private final String description;
  private final DataType dataType;
  private final List<String> enumVals;

  private AnnotationKey(
      String id, String displayName, String description, DataType dataType, List<String> enumVals) {
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

  public DataType getDataType() {
    return dataType;
  }

  public List<String> getEnumVals() {
    return enumVals;
  }

  public void validateValue(Literal annotationValue) {
    if (!annotationValue.isNull()) {
      if (!annotationValue.getDataType().equals(getDataType())) {
        throw new BadRequestException(
            String.format(
                "Annotation value data type (%s) does not match the annotation key data type (%s)",
                annotationValue.getDataType(), getDataType()));
      }

      switch (annotationValue.getDataType()) {
        case STRING:
          if (annotationValue.getStringVal() == null) {
            throw new BadRequestException("String value cannot be null");
          }
          break;
        case INT64:
          if (annotationValue.getInt64Val() == null) {
            throw new BadRequestException("Integer value cannot be null");
          }
          break;
        case BOOLEAN:
          if (annotationValue.getBooleanVal() == null) {
            throw new BadRequestException("Boolean value cannot be null");
          }
          break;
        case DATE:
          if (annotationValue.getDateVal() == null) {
            throw new BadRequestException("Date value cannot be null");
          }
          break;
        case DOUBLE:
          if (annotationValue.getDoubleVal() == null) {
            throw new BadRequestException("Double value cannot be null");
          }
          break;
        default:
          throw new SystemException(
              "Unknown annotation value data type: " + annotationValue.getDataType());
      }
    }

    if (!getEnumVals().isEmpty() && !getEnumVals().contains(annotationValue.getStringVal())) {
      throw new BadRequestException(
          String.format(
              "Annotation value (%s) is not one of the annotation enum values (%s)",
              annotationValue.getStringVal(), String.join(",", getEnumVals())));
    }
  }

  public static class Builder {
    private String id;
    private String displayName;
    private String description;
    private DataType dataType;
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

    public Builder dataType(DataType dataType) {
      this.dataType = dataType;
      return this;
    }

    public Builder enumVals(List<String> enumVals) {
      this.enumVals = enumVals;
      return this;
    }

    public void addEnumVal(String enumVal) {
      if (enumVals == null) {
        enumVals = new ArrayList<>();
      }
      enumVals.add(enumVal);
    }

    public AnnotationKey build() {
      if (id == null) {
        id = RandomStringUtils.randomAlphanumeric(10);
      }
      if (displayName == null) {
        throw new BadRequestException("Annotation key requires a display name");
      }
      if (displayName.length() > MAX_DISPLAY_NAME_LENGTH) {
        throw new BadRequestException(
            "Annotation key cannot be greater than " + MAX_DISPLAY_NAME_LENGTH + " characters");
      }
      if (enumVals == null) {
        enumVals = new ArrayList<>();
      }
      return new AnnotationKey(id, displayName, description, dataType, enumVals);
    }

    public String getId() {
      return id;
    }
  }
}
