package bio.terra.tanagra.query2.sql;

import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.underlay.entitymodel.Attribute;

public interface SqlFilterTranslator {
  String buildSql(SqlParams sqlParams, String tableAlias, FieldPointer idField);

  boolean isFilterOnAttribute(Attribute attribute);
}
