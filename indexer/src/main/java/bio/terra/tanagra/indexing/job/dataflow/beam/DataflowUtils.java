package bio.terra.tanagra.indexing.job.dataflow.beam;

import bio.terra.tanagra.underlay2.serialization.SZIndexer;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.apache.beam.runners.dataflow.DataflowRunner;
import org.apache.beam.runners.dataflow.options.DataflowPipelineOptions;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.vendor.guava.v26_0_jre.com.google.common.base.MoreObjects;
import org.apache.commons.text.StringSubstitutor;
import org.joda.time.DateTimeUtils;
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
   * 32 job, 5 user, 8 date, 4 dashes
   */
  private static String getJobName(String underlay, String job) {
    String cleanUnderlay = underlay.toLowerCase().replaceAll("[^a-z0-9]", "").substring(0, 15);
    String cleanJob = job.toLowerCase().replaceAll("[^a-z0-9]", "").substring(0, 32);
    String cleanUser =
        MoreObjects.firstNonNull(System.getProperty("user.name"), "")
            .toLowerCase()
            .replaceAll("[^a-z0-9]", "")
            .substring(0, 5);
    String datePart = FORMATTER.print(DateTimeUtils.currentTimeMillis());

    return String.format("%s-%s-%s-%s-%s", cleanUnderlay, cleanJob, cleanUser, datePart);
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
