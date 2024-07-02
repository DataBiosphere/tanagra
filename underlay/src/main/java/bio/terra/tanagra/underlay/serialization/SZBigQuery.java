package bio.terra.tanagra.underlay.serialization;

import bio.terra.tanagra.annotation.AnnotatedClass;
import bio.terra.tanagra.annotation.AnnotatedField;
import com.google.common.collect.*;
import jakarta.annotation.*;
import java.util.*;

@AnnotatedClass(
    name = "SZBigQuery",
    markdown = "Pointers to the source and index BigQuery datasets.")
public class SZBigQuery {
  @AnnotatedField(
      name = "SZBigQuery.sourceData",
      markdown = "Pointer to the source BigQuery dataset.")
  public SourceData sourceData;

  @AnnotatedField(
      name = "SZBigQuery.indexData",
      markdown = "Pointer to the index BigQuery dataset.")
  public IndexData indexData;

  @AnnotatedField(
      name = "SZBigQuery.queryProjectId",
      markdown =
          "Queries will be run in this project.\n\n"
              + "This is the project that will be billed for running queries. For the indexer, this project "
              + "is also where the Dataflow jobs will be kicked off. Often this project will be the same project "
              + "as the one where the index and/or source datasets live.\n\n"
              + "However, sometimes it will be different. For example, the source dataset may be a public "
              + "dataset that we don't have billing access to. In that case, the indexer configuration must "
              + "specify a different query project id. As another example, the source and index datasets may "
              + "live in a project that is shared across service deployments. In that case, the service "
              + "configurations may specify a different query project id for each deployment.")
  public String queryProjectId;

  @AnnotatedField(
      name = "SZBigQuery.dataLocation",
      markdown =
          "Valid locations for BigQuery are listed in the GCP "
              + "[documentation](https://cloud.google.com/bigquery/docs/locations).")
  public String dataLocation;

  @AnnotatedField(
      name = "SZBigQuery.exportDatasetIds",
      markdown =
          "Comma separated list of all BQ dataset ids that all export models can use. "
              + "Required if there are any export models that need to export from BQ to GCS.\n\n"
              + "These datasets must live in the [query project](${SZBigQuery.queryProjectId}) specified above.\n\n"
              + "You can also specify these export datasets per-deployment, instead of per-underlay, by using\n\n"
              + "the service application properties.",
      optional = true,
      exampleValue = "service_export_us,service_export_uscentral1")
  public List<String> exportDatasetIds;

  @AnnotatedField(
      name = "SZBigQuery.exportBucketNames",
      markdown =
          "Comma separated list of all GCS bucket names that all export models can use. "
              + "Only include the bucket name, not the gs:// prefix. "
              + "Required if there are any export models that need to write to GCS.\n\n"
              + "These buckets must live in the [query project](${SZBigQuery.queryProjectId}) specified above.\n\n"
              + "You can also specify these export buckets per-deployment, instead of per-underlay, by using\n\n"
              + "the service application properties.",
      optional = true,
      exampleValue = "bq-export-uscentral1,bq-export-useast1")
  public List<String> exportBucketNames;

  @AnnotatedClass(name = "SZSourceData", markdown = "Pointer to the source BigQuery dataset.")
  public static class SourceData {

    @AnnotatedField(
        name = "SZSourceData.projectId",
        markdown = "Project id of the source BigQuery dataset.")
    public String projectId;

    @AnnotatedField(
        name = "SZSourceData.datasetId",
        markdown = "Dataset id of the source BigQuery dataset.")
    public String datasetId;

    @AnnotatedField(
        name = "SZSourceData.sqlSubstitutions",
        markdown =
            "Key-value map of substitutions to make in the input SQL files.\n\n"
                + "Wherever the keys appear in the input SQL files wrapped in braces and preceded by a dollar "
                + "sign, they will be substituted by the values before running the queries. For example, [key] "
                + "`omopDataset` -> [value] `bigquery-public-data.cms_synthetic_patient_data_omop` means "
                + "`${omopDataset}` in any of the input SQL files will be replaced by "
                + "`bigquery-public-data.cms_synthetic_patient_data_omop`.\n\n"
                + "Keys may not include spaces or special characters, only letters and numbers. This is "
                + "simple string substitution logic and does not handle more complicated cases, such as nested "
                + "substitutions.",
        optional = true)
    public Map<String, String> sqlSubstitutions;
  }

  @AnnotatedClass(name = "SZIndexData", markdown = "Pointer to the index BigQuery dataset.")
  public static class IndexData {
    @AnnotatedField(
        name = "SZIndexData.projectId",
        markdown = "Project id of the index BigQuery dataset.")
    public String projectId;

    @AnnotatedField(
        name = "SZIndexData.datasetId",
        markdown = "Dataset id of the index BigQuery dataset.")
    public String datasetId;

    @AnnotatedField(
        name = "SZIndexData.tablePrefix",
        markdown =
            "Prefix for the generated index tables.\n\n"
                + "An underscore will be inserted between this prefix and the table name (e.g. prefix `T` "
                + "will generate a table called \"T_ENT_person\"). The prefix may not include spaces or special "
                + "characters, only letters and numbers. The first character must be a letter. This can be "
                + "useful when the index tables will be written to a dataset that includes other non-Tanagra "
                + "tables.",
        optional = true)
    public String tablePrefix;
  }
}
