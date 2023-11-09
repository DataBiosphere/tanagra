package bio.terra.tanagra.api.query.count;

import bio.terra.tanagra.api.field.AttributeField;
import bio.terra.tanagra.api.field.EntityIdCountField;
import bio.terra.tanagra.api.field.ValueDisplayField;
import bio.terra.tanagra.api.query.ValueDisplay;
import bio.terra.tanagra.api.query.hint.HintInstance;
import bio.terra.tanagra.api.query.hint.HintQueryResult;
import bio.terra.tanagra.query.RowResult;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CountInstance {
  private static final Logger LOGGER = LoggerFactory.getLogger(CountInstance.class);
  private final long count;
  private final ImmutableMap<ValueDisplayField, ValueDisplay> entityFieldValues;

  private CountInstance(long count, Map<ValueDisplayField, ValueDisplay> entityFieldValues) {
    this.count = count;
    this.entityFieldValues = ImmutableMap.copyOf(entityFieldValues);
  }

  public static CountInstance fromRowResult(
      RowResult rowResult,
      List<ValueDisplayField> groupByFields,
      EntityIdCountField entityIdCountField,
      HintQueryResult entityLevelHints) {
    ValueDisplay countIdFieldValue = entityIdCountField.parseFromRowResult(rowResult);
    long count = countIdFieldValue.getValue().getInt64Val();

    Map<ValueDisplayField, ValueDisplay> groupByFieldValues = new HashMap<>();
    groupByFields.stream()
        .forEach(
            entityField -> {
              ValueDisplay fieldValue = entityField.parseFromRowResult(rowResult);
              if (entityField instanceof AttributeField) {
                AttributeField attributeField = (AttributeField) entityField;
                Attribute attribute = attributeField.getAttribute();
                if (attribute.isValueDisplay() && attributeField.isExcludeDisplay()) {
                  // Populate the display from the entity-level hints.
                  Optional<HintInstance> attributeHint =
                      entityLevelHints.getHintInstance(attribute);
                  if (attributeHint.isPresent() && attributeHint.get().isEnumHint()) {
                    Optional<String> display =
                        attributeHint.get().getEnumDisplay(fieldValue.getValue());
                    if (display.isPresent()) {
                      fieldValue = new ValueDisplay(fieldValue.getValue(), display.get());
                    } else {
                      LOGGER.debug(
                          "Entity-level hint found for attribute {}, but no display present. Unable to populate display for count query results.",
                          attribute.getName());
                    }
                  } else {
                    LOGGER.debug(
                        "Entity-level hint not found for attribute {}. Unable to populate display for count query results.",
                        attribute.getName());
                  }
                }
              }
              groupByFieldValues.put(entityField, fieldValue);
            });
    return new CountInstance(count, groupByFieldValues);
  }

  public long getCount() {
    return count;
  }

  public ImmutableMap<ValueDisplayField, ValueDisplay> getEntityFieldValues() {
    return entityFieldValues;
  }
}
