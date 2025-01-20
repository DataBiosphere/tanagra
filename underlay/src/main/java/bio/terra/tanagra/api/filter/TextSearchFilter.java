package bio.terra.tanagra.api.filter;

import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import jakarta.annotation.Nullable;
import java.util.Objects;
import org.slf4j.LoggerFactory;

public class TextSearchFilter extends EntityFilter {

  public enum TextSearchOperator {
    EXACT_MATCH,
    FUZZY_MATCH
  }

  private final TextSearchOperator operator;
  private final String text;
  private final @Nullable Attribute attribute;

  public TextSearchFilter(
      Underlay underlay,
      Entity entity,
      TextSearchOperator operator,
      String text,
      @Nullable Attribute attribute) {
    super(LoggerFactory.getLogger(TextSearchFilter.class), underlay, entity);
    this.operator = operator;
    this.text = text;
    this.attribute = attribute;
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
    if (!super.equals(o)) {
      return false;
    }
    TextSearchFilter that = (TextSearchFilter) o;
    return operator == that.operator
        && text.equals(that.text)
        && Objects.equals(attribute, that.attribute);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), operator, text, attribute);
  }
}
