package bio.terra.tanagra.underlay2;

import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.underlay2.indexschema.EntityMain;
import com.google.common.collect.ImmutableList;
import java.util.List;
import javax.annotation.Nullable;

public class TextSearch {
  private final ImmutableList<Attribute> searchAttributes;
  private final @Nullable FieldPointer sourceAdditionalTermsIdField;
  private final @Nullable FieldPointer sourceAdditionalTermsTextField;
  private final FieldPointer indexCombinedSearchField;

  public TextSearch(
      String entity,
      @Nullable List<Attribute> searchAttributes,
      @Nullable FieldPointer sourceAdditionalTermsIdField,
      @Nullable FieldPointer sourceAdditionalTermsTextField) {
    this.searchAttributes =
        searchAttributes == null ? ImmutableList.of() : ImmutableList.copyOf(searchAttributes);
    this.sourceAdditionalTermsIdField = sourceAdditionalTermsIdField;
    this.sourceAdditionalTermsTextField = sourceAdditionalTermsTextField;

    // Resolve index field.
    this.indexCombinedSearchField = EntityMain.getTextCombinedSearchField(entity);
  }

  public ImmutableList<Attribute> getSearchAttributes() {
    return searchAttributes;
  }

  @Nullable
  public FieldPointer getSourceAdditionalTermsIdField() {
    return sourceAdditionalTermsIdField;
  }

  @Nullable
  public FieldPointer getSourceAdditionalTermsTextField() {
    return sourceAdditionalTermsTextField;
  }

  public FieldPointer getIndexCombinedSearchField() {
    return indexCombinedSearchField;
  }
}
