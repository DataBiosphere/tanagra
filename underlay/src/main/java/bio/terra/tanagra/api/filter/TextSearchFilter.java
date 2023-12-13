package bio.terra.tanagra.api.filter;

import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.query.FieldVariable;
import bio.terra.tanagra.query.FilterVariable;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.query.TableVariable;
import bio.terra.tanagra.query.filtervariable.FunctionFilterVariable;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.indextable.ITEntityMain;
import java.util.List;
import javax.annotation.Nullable;

public class TextSearchFilter extends EntityFilter {
  private final Entity entity;
  private final ITEntityMain indexTable;
  private final FunctionFilterVariable.FunctionTemplate functionTemplate;
  private final String text;
  private final @Nullable Attribute attribute;

  public TextSearchFilter(
      Underlay underlay,
      Entity entity,
      FunctionFilterVariable.FunctionTemplate functionTemplate,
      String text,
      @Nullable Attribute attribute) {
    this.entity = entity;
    this.indexTable = underlay.getIndexSchema().getEntityMain(entity.getName());
    this.functionTemplate = functionTemplate;
    this.text = text;
    this.attribute = attribute;
  }

  @Override
  public FilterVariable getFilterVariable(
      TableVariable entityTableVar, List<TableVariable> tableVars) {
    // If a specific attribute is defined, search only that field.
    // Otherwise, search the combined text string field.
    FieldPointer searchField =
        attribute == null
            ? indexTable.getTextSearchField()
            : (attribute.isValueDisplay()
                ? indexTable.getAttributeDisplayField(attribute.getName())
                : indexTable.getAttributeValueField(attribute.getName()));
    return new FunctionFilterVariable(
        functionTemplate, new FieldVariable(searchField, entityTableVar), new Literal(text));
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

  public FunctionFilterVariable.FunctionTemplate getFunctionTemplate() {
    return functionTemplate;
  }

  public String getText() {
    return text;
  }
}
