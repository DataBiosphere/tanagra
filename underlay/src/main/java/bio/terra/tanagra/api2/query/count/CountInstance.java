package bio.terra.tanagra.api2.query.count;

import bio.terra.tanagra.api2.field.EntityField;
import bio.terra.tanagra.api2.field.EntityIdCountField;
import bio.terra.tanagra.api2.query.ValueDisplay;
import bio.terra.tanagra.query.RowResult;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CountInstance {
  private final long count;
  private final ImmutableMap<EntityField, ValueDisplay> entityFieldValues;

  private CountInstance(long count, Map<EntityField, ValueDisplay> entityFieldValues) {
    this.count = count;
    this.entityFieldValues = ImmutableMap.copyOf(entityFieldValues);
  }

  public static CountInstance fromRowResult(
      RowResult rowResult, List<EntityField> groupByFields, EntityIdCountField entityIdCountField) {
    ValueDisplay countIdFieldValue = entityIdCountField.parseFromRowResult(rowResult);
    long count = countIdFieldValue.getValue().getInt64Val();

    Map<EntityField, ValueDisplay> groupByFieldValues = new HashMap<>();
    groupByFields.stream()
        .forEach(
            entityField ->
                groupByFieldValues.put(entityField, entityField.parseFromRowResult(rowResult)));
    return new CountInstance(count, groupByFieldValues);
  }

  public long getCount() {
    return count;
  }

  public ImmutableMap<EntityField, ValueDisplay> getEntityFieldValues() {
    return entityFieldValues;
  }
}
