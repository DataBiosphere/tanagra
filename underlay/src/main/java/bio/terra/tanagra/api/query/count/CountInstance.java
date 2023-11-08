package bio.terra.tanagra.api.query.count;

import bio.terra.tanagra.api.field.EntityIdCountField;
import bio.terra.tanagra.api.field.ValueDisplayField;
import bio.terra.tanagra.api.query.ValueDisplay;
import bio.terra.tanagra.query.RowResult;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CountInstance {
  private final long count;
  private final ImmutableMap<ValueDisplayField, ValueDisplay> entityFieldValues;

  private CountInstance(long count, Map<ValueDisplayField, ValueDisplay> entityFieldValues) {
    this.count = count;
    this.entityFieldValues = ImmutableMap.copyOf(entityFieldValues);
  }

  public static CountInstance fromRowResult(
      RowResult rowResult,
      List<ValueDisplayField> groupByFields,
      EntityIdCountField entityIdCountField) {
    ValueDisplay countIdFieldValue = entityIdCountField.parseFromRowResult(rowResult);
    long count = countIdFieldValue.getValue().getInt64Val();

    Map<ValueDisplayField, ValueDisplay> groupByFieldValues = new HashMap<>();
    groupByFields.stream()
        .forEach(
            entityField ->
                groupByFieldValues.put(entityField, entityField.parseFromRowResult(rowResult)));
    return new CountInstance(count, groupByFieldValues);
  }

  public long getCount() {
    return count;
  }

  public ImmutableMap<ValueDisplayField, ValueDisplay> getEntityFieldValues() {
    return entityFieldValues;
  }
}
