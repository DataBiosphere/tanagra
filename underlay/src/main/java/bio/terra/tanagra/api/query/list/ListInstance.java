package bio.terra.tanagra.api.query.list;

import bio.terra.tanagra.api.field.valuedisplay.ValueDisplayField;
import bio.terra.tanagra.api.query.ValueDisplay;
import bio.terra.tanagra.query.RowResult;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ListInstance {
  private final ImmutableMap<ValueDisplayField, ValueDisplay> entityFieldValues;

  public ListInstance(Map<ValueDisplayField, ValueDisplay> entityFieldValues) {
    this.entityFieldValues = ImmutableMap.copyOf(entityFieldValues);
  }

  public static ListInstance fromRowResult(
      RowResult rowResult, List<ValueDisplayField> selectFields) {
    Map<ValueDisplayField, ValueDisplay> selectFieldValues = new HashMap<>();
    selectFields.stream()
        .forEach(
            entityField ->
                selectFieldValues.put(entityField, entityField.parseFromRowResult(rowResult)));
    return new ListInstance(selectFieldValues);
  }

  public ImmutableMap<ValueDisplayField, ValueDisplay> getEntityFieldValues() {
    return entityFieldValues;
  }

  public ValueDisplay getEntityFieldValue(ValueDisplayField valueDisplayField) {
    return entityFieldValues.get(valueDisplayField);
  }
}
