package bio.terra.tanagra.api2.filter;

import bio.terra.tanagra.query.*;
import bio.terra.tanagra.query.filtervariable.FunctionFilterVariable;
import bio.terra.tanagra.underlay2.Attribute;
import bio.terra.tanagra.underlay2.TextSearch;
import java.util.List;
import javax.annotation.Nullable;

public class TextSearchFilter extends EntityFilter {
  private final TextSearch textSearch;
  private final FunctionFilterVariable.FunctionTemplate functionTemplate;
  private final String text;
  private final @Nullable Attribute attribute;

  public TextSearchFilter(
      TextSearch textSearch,
      FunctionFilterVariable.FunctionTemplate functionTemplate,
      String text,
      @Nullable Attribute attribute) {
    this.textSearch = textSearch;
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
            ? textSearch.getIndexCombinedSearchField()
            : (attribute.isValueDisplay()
                ? attribute.getIndexDisplayField()
                : attribute.getIndexValueField());
    return new FunctionFilterVariable(
        functionTemplate, new FieldVariable(searchField, entityTableVar), new Literal(text));
  }
}
