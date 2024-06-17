package bio.terra.tanagra.api.filter;

import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import java.util.Objects;
import javax.annotation.Nullable;

public class TextSearchFilter extends EntityFilter {
  public enum TextSearchOperator {
    EXACT_MATCH,
    FUZZY_MATCH
  }

  private final Underlay underlay;
  private final Entity entity;
  private final TextSearchOperator operator;
  private final String text;
  private final @Nullable Attribute attribute;

  public TextSearchFilter(
      Underlay underlay,
      Entity entity,
      TextSearchOperator operator,
      String text,
      @Nullable Attribute attribute) {
    this.underlay = underlay;
    this.entity = entity;
    this.operator = operator;
    this.text = text;
    this.attribute = attribute;
  }

  public Underlay getUnderlay() {
    return underlay;
  }

  public Entity getEntity() {
    return entity;
  }

  public boolean isForSpecificAttribute() {
    return attribute != null;
  }

  @Nullable
  public Attribute getAttribute() {
    return attribute;
  }

  public TextSearchOperator getOperator() {
    return operator;
  }

  public String getText() {
    return text;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TextSearchFilter that = (TextSearchFilter) o;
    return underlay.equals(that.underlay)
        && entity.equals(that.entity)
        && operator == that.operator
        && text.equals(that.text)
        && Objects.equals(attribute, that.attribute);
  }

  @Override
  public int hashCode() {
    return Objects.hash(underlay, entity, operator, text, attribute);
  }
}
