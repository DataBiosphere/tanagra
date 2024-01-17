package bio.terra.tanagra.api.filter;

import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.Entity;
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
}
