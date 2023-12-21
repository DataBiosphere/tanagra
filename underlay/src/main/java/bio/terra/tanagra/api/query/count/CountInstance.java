package bio.terra.tanagra.api.query.count;

import bio.terra.tanagra.api.field.ValueDisplayField;
import bio.terra.tanagra.api.query.ValueDisplay;
import com.google.common.collect.ImmutableMap;
import java.util.Map;

public final class CountInstance {
  private final long count;
  private final ImmutableMap<ValueDisplayField, ValueDisplay> entityFieldValues;

  public CountInstance(long count, Map<ValueDisplayField, ValueDisplay> entityFieldValues) {
    this.count = count;
    this.entityFieldValues = ImmutableMap.copyOf(entityFieldValues);
  }

  public long getCount() {
    return count;
  }

  public ImmutableMap<ValueDisplayField, ValueDisplay> getEntityFieldValues() {
    return entityFieldValues;
  }

  public ValueDisplay getEntityFieldValue(ValueDisplayField valueDisplayField) {
    return entityFieldValues.get(valueDisplayField);
  }
}
