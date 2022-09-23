package bio.terra.tanagra.serialization.tablefilter;

import bio.terra.tanagra.serialization.UFTableFilter;
import bio.terra.tanagra.underlay.TableFilter;
import bio.terra.tanagra.underlay.TablePointer;
import bio.terra.tanagra.underlay.tablefilter.ArrayFilter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.List;
import java.util.stream.Collectors;

/**
 * External representation of an array table filter: subfilter1 operator subfilter2 operator ...
 * (e.g. domain_id = "Condition" AND is_expired = FALSE).
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 */
@JsonDeserialize(builder = UFArrayFilter.Builder.class)
public class UFArrayFilter extends UFTableFilter {
  private final TableFilter.LogicalOperator operator;
  private final List<UFTableFilter> subfilters;

  public UFArrayFilter(ArrayFilter arrayFilter) {
    super(arrayFilter);
    this.operator = arrayFilter.getOperator();
    this.subfilters =
        arrayFilter.getSubfilters().stream().map(sf -> sf.serialize()).collect(Collectors.toList());
  }

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
    @Override
    public UFArrayFilter build() {
      return new UFArrayFilter(this);
    }
  }

  /** Deserialize to the internal representation of the table filter. */
  @Override
  public TableFilter deserializeToInternal(TablePointer tablePointer) {
    return ArrayFilter.fromSerialized(this, tablePointer);
  }

  public TableFilter.LogicalOperator getOperator() {
    return operator;
  }

  public List<UFTableFilter> getSubfilters() {
    return subfilters;
  }
}
