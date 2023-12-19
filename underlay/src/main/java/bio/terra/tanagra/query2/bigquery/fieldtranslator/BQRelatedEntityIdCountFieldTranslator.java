package bio.terra.tanagra.query2.bigquery.fieldtranslator;

import bio.terra.tanagra.api.field.valuedisplay.EntityIdCountField;
import bio.terra.tanagra.api.field.valuedisplay.HierarchyIsMemberField;
import bio.terra.tanagra.api.field.valuedisplay.HierarchyIsRootField;
import bio.terra.tanagra.api.field.valuedisplay.HierarchyNumChildrenField;
import bio.terra.tanagra.api.field.valuedisplay.HierarchyPathField;
import bio.terra.tanagra.api.field.valuedisplay.RelatedEntityIdCountField;
import bio.terra.tanagra.api.query.ValueDisplay;
import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.query2.sql.SqlField;
import bio.terra.tanagra.query2.sql.SqlFieldTranslator;
import bio.terra.tanagra.underlay.NameHelper;
import bio.terra.tanagra.underlay.indextable.ITEntityMain;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public class BQRelatedEntityIdCountFieldTranslator implements SqlFieldTranslator {
    private final RelatedEntityIdCountField relatedEntityIdCountField;
    public BQRelatedEntityIdCountFieldTranslator(RelatedEntityIdCountField relatedEntityIdCountField) {
        this.relatedEntityIdCountField = relatedEntityIdCountField;
    }

    @Override
    public List<SqlField> buildSqlFieldsForListSelect() {
        return buildSqlFields();
    }

    @Override
    public List<SqlField> buildSqlFieldsForCountSelect() {
        return buildSqlFields();
    }

    @Override
    public List<SqlField> buildSqlFieldsForOrderBy() {
        return buildSqlFields();
    }

    @Override
    public List<SqlField> buildSqlFieldsForGroupBy() {
        return buildSqlFields();
    }
    private List<SqlField> buildSqlFields() {
        return List.of(SqlField.of(getField(), null));
    }
    private FieldPointer getField() {
        ITEntityMain indexTable = relatedEntityIdCountField.getUnderlay().getIndexSchema().getEntityMain(relatedEntityIdCountField.getCountForEntity().getName());
        return indexTable.getEntityGroupCountField(relatedEntityIdCountField.getEntityGroup().getName(), relatedEntityIdCountField.hasHierarchy() ? relatedEntityIdCountField.getHierarchy().getName() : null);}

    @Override
    public ValueDisplay parseValueDisplayFromResult() {
        return null;
    }
}
