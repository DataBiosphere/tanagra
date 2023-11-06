package bio.terra.tanagra.indexing.job.dataflow.beam;

import bio.terra.tanagra.underlay2.serialization.SZIndexer;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.apache.beam.runners.dataflow.DataflowRunner;
import org.apache.beam.runners.dataflow.options.DataflowPipelineOptions;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.commons.text.StringSubstitutor;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public final class DataflowUtils {
  private static final DateTimeFormatter FORMATTER =
      DateTimeFormat.forPattern("MMddHHmm").withZone(DateTimeZone.UTC);

  private DataflowUtils() {}

  public static BigQueryOptions getPipelineOptions(SZIndexer indexerConfig, String jobName) {
    DataflowPipelineOptions dataflowOptions =
        PipelineOptionsFactory.create().as(DataflowPipelineOptions.class);
    dataflowOptions.setRunner(DataflowRunner.class);
    dataflowOptions.setProject(indexerConfig.bigQuery.queryProjectId);
    dataflowOptions.setRegion(indexerConfig.bigQuery.dataLocation);
    dataflowOptions.setServiceAccount(indexerConfig.dataflow.serviceAccountEmail);
    dataflowOptions.setJobName(getJobName(indexerConfig.underlay, jobName));
    dataflowOptions.setUsePublicIps(indexerConfig.dataflow.usePublicIps);
    dataflowOptions.setWorkerMachineType(indexerConfig.dataflow.workerMachineType);
    dataflowOptions.setTempLocation(indexerConfig.dataflow.gcsTempDirectory);
    if (indexerConfig.dataflow.vpcSubnetworkName != null
        && !indexerConfig.dataflow.vpcSubnetworkName.isEmpty()) {
      dataflowOptions.setSubnetwork(
          getSubnetworkName(
              indexerConfig.bigQuery.queryProjectId,
              indexerConfig.bigQuery.dataLocation,
              indexerConfig.dataflow.vpcSubnetworkName));
    }
    return dataflowOptions;
  }

  /**
   * Dataflow job name should be a valid cloud resource label.
   * https://cloud.google.com/compute/docs/labeling-resources#requirements 64 chars = 15 underlay,
   * 40 job, 6 user, 3 dashes
   */
  private static String getJobName(String underlay, String job) {
    String cleanUnderlay = underlay.toLowerCase().replaceAll("[^a-z0-9]", "");
    if (cleanUnderlay.length() > 15) {
      cleanUnderlay = cleanUnderlay.substring(0, 15);
    }
    String cleanJob = job.toLowerCase().replaceAll("[^a-z0-9-]", "");
    if (cleanJob.length() > 40) {
      cleanJob = cleanJob.substring(0, 40);
    }
    String systemUserName = System.getProperty("user.name");
    String cleanUser =
        (systemUserName == null ? "" : systemUserName).toLowerCase().replaceAll("[^a-z0-9]", "");
    if (cleanUser.length() > 6) {
      cleanUser = cleanUser.substring(0, 6);
    }

    return String.format("%s-%s-%s", cleanUnderlay, cleanJob, cleanUser);
  }

  /**
   * Get the full name of a VPC subnetwork.
   * https://cloud.google.com/dataflow/docs/guides/specifying-networks#example_network_and_subnetwork_specifications
   */
  private static String getSubnetworkName(String project, String region, String subnetwork) {
    String template =
        "https://www.googleapis.com/compute/v1/projects/${project}/regions/${region}/subnetworks/${subnetwork}";
    Map<String, String> params =
        ImmutableMap.<String, String>builder()
            .put("project", project)
            .put("region", region)
            .put("subnetwork", subnetwork)
            .build();
    return StringSubstitutor.replace(template, params);
  }
}
