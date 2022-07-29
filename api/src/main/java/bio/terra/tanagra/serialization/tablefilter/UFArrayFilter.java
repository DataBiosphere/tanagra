package bio.terra.tanagra.serialization.tablefilter;

import bio.terra.tanagra.serialization.UFTableFilter;
import bio.terra.tanagra.underlay.TableFilter;
import bio.terra.tanagra.underlay.TablePointer;
import bio.terra.tanagra.underlay.tablefilter.ArrayFilter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.List;

/**
 * External representation of an array table filter: subfilter1 operator subfilter2 operator ...
 * (e.g. domain_id = "Condition" AND is_expired = FALSE).
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 */
@JsonDeserialize(builder = UFArrayFilter.Builder.class)
public class UFArrayFilter extends UFTableFilter {
  public final TableFilter.LogicalOperator operator;
  public final List<UFTableFilter> subfilters;

  /** Constructor for Jackson deserialization during testing. */
  private UFArrayFilter(Builder builder) {
    super(builder);
    this.operator = builder.operator;
    this.subfilters = builder.subfilters;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder extends UFTableFilter.Builder {
    private TableFilter.LogicalOperator operator;
    private List<UFTableFilter> subfilters;

    public Builder operator(TableFilter.LogicalOperator operator) {
      this.operator = operator;
      return this;
    }

    public Builder subfilters(List<UFTableFilter> subfilters) {
      this.subfilters = subfilters;
      return this;
    }

    /** Call the private constructor. */
    public UFArrayFilter build() {
      return new UFArrayFilter(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}
  }

  @Override
  public TableFilter deserializeToInternal(TablePointer tablePointer) {
    return ArrayFilter.fromSerialized(this, tablePointer);
  }
}
