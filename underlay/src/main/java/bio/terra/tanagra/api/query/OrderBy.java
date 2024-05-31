package bio.terra.tanagra.api.query;

import bio.terra.tanagra.api.field.ValueDisplayField;
import bio.terra.tanagra.api.shared.OrderByDirection;
import javax.annotation.Nullable;

public class OrderBy {
  private final @Nullable ValueDisplayField valueDisplayField;
  private final @Nullable OrderByDirection direction;
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

  public ValueDisplayField getEntityField() {
    return valueDisplayField;
  }

  public OrderByDirection getDirection() {
    return direction;
  }

  public boolean isRandom() {
    return isRandom;
  }
}
