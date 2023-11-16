package bio.terra.tanagra.underlay.serialization;

import java.util.Map;

/** Pointers to the source and index BigQuery datasets. */
public class SZBigQuery {
  /** Pointer to the source BigQuery dataset. */
  public SourceData sourceData;

  /** Pointer to the index BigQuery dataset. */
  public IndexData indexData;

  /**
   * Queries will be run in this project.
   *
   * <p>This is the project that will be billed for running queries. For the indexer, this project
   * is also where the Dataflow jobs will be kicked off. Often this project will be the same project
   * as the one where the index and/or source datasets live.
   *
   * <p>However, sometimes it will be different. For example, the source dataset may be a public
   * dataset that we don't have billing access to. In that case, the indexer configuration must
   * specify a different query project id. As another example, the source and index datasets may
   * live in a project that is shared across service deployments. In that case, the service
   * configurations may specify a different query project id for each deployment.
   */
  public String queryProjectId;

  /**
   * Location where both the source and index datasets live.
   *
   * <p>Valid locations for BigQuery are listed in the GCP <a
   * href="https://cloud.google.com/bigquery/docs/locations">documentation</a>.
   */
  public String dataLocation;

  /** Pointer to the source BigQuery dataset. */
  public static class SourceData {
    /** Project id of the source BigQuery dataset. */
    public String projectId;

    /** Dataset id of the source BigQuery dataset. */
    public String datasetId;

    /**
     * <strong>(optional)</strong> Key-value map of substitutions to make in the input SQL files.
     *
     * <p>Wherever the keys appear in the input SQL files wrapped in braces and preceded by a dollar
     * sign, they will be substituted by the values before running the queries. For example, [key]
     * omopDataset -> [value] bigquery-public-data.cms_synthetic_patient_data_omop means
     * ${omopDataset} in any of the input SQL files will be replaced by
     * bigquery-public-data.cms_synthetic_patient_data_omop.
     *
     * <p>Keys may not include spaces or special characters, only letters and numbers. This is
     * simple string substitution logic and does not handle more complicated cases, such as nested
     * substitutions.
     */
    public Map<String, String> sqlSubstitutions;
  }

  /** Pointer to the index BigQuery dataset. */
  public static class IndexData {
    /** Project id of the index BigQuery dataset. */
    public String projectId;

    /** Dataset id of the index BigQuery dataset. */
    public String datasetId;

    /**
     * <strong>(optional)</strong> Prefix for the generated index tables.
     *
     * <p>An underscore will be inserted between this prefix and the table name (e.g. prefix "T"
     * will generate a table called "T_ENT_person"). The prefix may not include spaces or special
     * characters, only letters and numbers. The first character must be a letter. This can be
     * useful when the index tables will be written to a dataset that includes other non-Tanagra
     * tables.
     */
    public String tablePrefix;
  }
}
