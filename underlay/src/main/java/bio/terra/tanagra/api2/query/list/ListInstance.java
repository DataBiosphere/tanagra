package bio.terra.tanagra.api2.query.list;

import bio.terra.tanagra.api2.field.EntityField;
import bio.terra.tanagra.api2.query.ValueDisplay;
import bio.terra.tanagra.query.RowResult;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ListInstance {
  private final ImmutableMap<EntityField, ValueDisplay> entityFieldValues;

  private ListInstance(Map<EntityField, ValueDisplay> entityFieldValues) {
    this.entityFieldValues = ImmutableMap.copyOf(entityFieldValues);
  }

  public static ListInstance fromRowResult(RowResult rowResult, List<EntityField> selectFields) {
    Map<EntityField, ValueDisplay> selectFieldValues = new HashMap<>();
    selectFields.stream()
        .forEach(
            entityField ->
                selectFieldValues.put(entityField, entityField.parseFromRowResult(rowResult)));
    return new ListInstance(selectFieldValues);
  }

  public ImmutableMap<EntityField, ValueDisplay> getEntityFieldValues() {
    return entityFieldValues;
  }
}
