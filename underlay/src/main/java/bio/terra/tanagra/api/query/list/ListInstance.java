package bio.terra.tanagra.api.query.list;

import bio.terra.tanagra.api.field.ValueDisplayField;
import bio.terra.tanagra.api.shared.ValueDisplay;
import com.google.common.collect.ImmutableMap;
import java.util.Map;

public final class ListInstance {
  private final ImmutableMap<ValueDisplayField, ValueDisplay> entityFieldValues;

  public ListInstance(Map<ValueDisplayField, ValueDisplay> entityFieldValues) {
    this.entityFieldValues = ImmutableMap.copyOf(entityFieldValues);
  }

  public ImmutableMap<ValueDisplayField, ValueDisplay> getEntityFieldValues() {
    return entityFieldValues;
  }

  public ValueDisplay getEntityFieldValue(ValueDisplayField valueDisplayField) {
    return entityFieldValues.get(valueDisplayField);
  }
}
