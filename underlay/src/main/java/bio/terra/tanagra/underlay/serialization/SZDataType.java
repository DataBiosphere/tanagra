package bio.terra.tanagra.underlay.serialization;

import bio.terra.tanagra.annotation.AnnotatedClass;
import bio.terra.tanagra.annotation.AnnotatedField;

@AnnotatedClass(
    name = "SZDataType",
    markdown =
        "Supported data types. Each type corresponds to one or more data types in the underlying database.")
public enum SZDataType {
  @AnnotatedField(name = "SZDataType.INT64", markdown = "Maps to BigQuery `INTEGER` data type.")
  INT64,

  @AnnotatedField(name = "SZDataType.STRING", markdown = "Maps to BigQuery `STRING` data type.")
  STRING,

  @AnnotatedField(name = "SZDataType.BOOLEAN", markdown = "Maps to BigQuery `BOOLEAN` data type.")
  BOOLEAN,

  @AnnotatedField(name = "SZDataType.DATE", markdown = "Maps to BigQuery `DATE` data type.")
  DATE,

  @AnnotatedField(
      name = "SZDataType.DOUBLE",
      markdown = "Maps to BigQuery `NUMERIC` and `FLOAT` data types.")
  DOUBLE,

  @AnnotatedField(
      name = "SZDataType.TIMESTAMP",
      markdown = "Maps to BigQuery `TIMESTAMP` data type.")
  TIMESTAMP
}
