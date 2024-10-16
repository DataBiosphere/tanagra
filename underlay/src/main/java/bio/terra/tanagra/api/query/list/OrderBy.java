package bio.terra.tanagra.api.query.list;

import bio.terra.tanagra.api.field.ValueDisplayField;
import bio.terra.tanagra.api.shared.OrderByDirection;
import jakarta.annotation.Nullable;

public class OrderBy {
  private final ValueDisplayField valueDisplayField;
  private final OrderByDirection direction;
  private final boolean isRandom;

  public OrderBy(ValueDisplayField valueDisplayField, OrderByDirection direction) {
    this(valueDisplayField, direction, false);
  }

  public static OrderBy random() {
    return new OrderBy(null, null, true);
  }

  private OrderBy(
      @Nullable ValueDisplayField valueDisplayField,
      @Nullable OrderByDirection direction,
      boolean isRandom) {
    this.valueDisplayField = valueDisplayField;
    this.direction = direction;
    this.isRandom = isRandom;
  }

  public @Nullable ValueDisplayField getEntityField() {
    return valueDisplayField;
  }

  public @Nullable OrderByDirection getDirection() {
    return direction;
  }

  public boolean isRandom() {
    return isRandom;
  }
}
