package bio.terra.tanagra.serialization;

import bio.terra.tanagra.underlay.AccessControlModel;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.Map;

/**
 * External representation of an access control model.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 */
@JsonDeserialize(builder = UFAccessControlModel.Builder.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UFAccessControlModel {
  private final AccessControlModel.Type type;
  private final Map<String, String> params;

  public UFAccessControlModel(AccessControlModel accessControlModel) {
    this.type = accessControlModel.getType();
    this.params = accessControlModel.getParams();
  }

  private UFAccessControlModel(Builder builder) {
    this.type = builder.type;
    this.params = builder.params;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private AccessControlModel.Type type;
    private Map<String, String> params;

    public Builder type(AccessControlModel.Type type) {
      this.type = type;
      return this;
    }

    public Builder params(Map<String, String> params) {
      this.params = params;
      return this;
    }

    public UFAccessControlModel build() {
      return new UFAccessControlModel(this);
    }
  }

  public AccessControlModel.Type getType() {
    return type;
  }

  public Map<String, String> getParams() {
    return params;
  }
}
