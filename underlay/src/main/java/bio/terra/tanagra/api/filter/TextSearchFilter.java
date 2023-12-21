package bio.terra.tanagra.api.filter;

import bio.terra.tanagra.api.shared.FunctionTemplate;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import javax.annotation.Nullable;

public class TextSearchFilter extends EntityFilter {
  private final Underlay underlay;
  private final Entity entity;
  private final FunctionTemplate functionTemplate;
  private final String text;
  private final @Nullable Attribute attribute;

  public TextSearchFilter(
      Underlay underlay,
      Entity entity,
      FunctionTemplate functionTemplate,
      String text,
      @Nullable Attribute attribute) {
    this.underlay = underlay;
    this.entity = entity;
    this.functionTemplate = functionTemplate;
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

  public FunctionTemplate getFunctionTemplate() {
    return functionTemplate;
  }

  public String getText() {
    return text;
  }
}
