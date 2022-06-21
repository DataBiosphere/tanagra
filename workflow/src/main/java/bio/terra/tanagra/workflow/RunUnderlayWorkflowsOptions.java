package bio.terra.tanagra.workflow;

import org.apache.beam.sdk.io.gcp.bigquery.BigQueryOptions;
import org.apache.beam.sdk.options.Description;

/** Options supported by {@link RunUnderlayWorkflows}. */
public interface RunUnderlayWorkflowsOptions extends BigQueryOptions {
  @Description("Path to the underlay definition to run the workflows for.")
  String getUnderlayPath();

  void setUnderlayPath(String path);

  @Description("Display commands that will be run without actually running them.")
  boolean isDryrun();

  void setDryrun(boolean dryrun);
}
