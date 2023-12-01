package bio.terra.tanagra.underlay.serialization;

import bio.terra.tanagra.annotation.AnnotatedClass;
import bio.terra.tanagra.annotation.AnnotatedField;

@AnnotatedClass(
    name = "SZIndexer",
    markdown =
        "Indexer configuration.\n\n"
            + "Define a version of this file for each place you will run indexing. If you later copy the "
            + "index dataset to other places, you do not need a separate configuration for those.")
public class SZIndexer {
  @AnnotatedField(
      name = "SZIndexer.underlay",
      markdown =
          "Name of the underlay to index.\n\n"
              + "Name is specified in the underlay file, and also matches the name of the config/underlay "
              + "sub-directory in the underlay sub-project resources.",
      exampleValue = "cmssynpuf")
  public String underlay;

  @AnnotatedField(
      name = "SZIndexer.bigQuery",
      markdown = "Pointers to the source and index BigQuery datasets.")
  public SZBigQuery bigQuery;

  @AnnotatedField(
      name = "SZIndexer.dataflow",
      markdown =
          "Dataflow configuration.\n\n"
              + "Required for indexing jobs that use batch processing (e.g. computing the ancestor-descendant "
              + "pairs for a hierarchy).")
  public Dataflow dataflow;

  @AnnotatedClass(
      name = "SZDataflow",
      markdown = "Properties to pass to Dataflow when kicking off jobs.")
  public static class Dataflow {
    @AnnotatedField(
        name = "SZDataflow.serviceAccountEmail",
        markdown =
            "Email of the service account that the Dataflow runners will use.\n\n"
                + "The credentials used to kickoff the indexing must have the `iam.serviceAccounts.actAs` "
                + "permission on this service account. More details in the "
                + "[GCP documentation](https://cloud.google.com/iam/docs/service-accounts-actas).")
    public String serviceAccountEmail;

    @AnnotatedField(
        name = "SZDataflow.dataflowLocation",
        markdown =
            "Location where the Dataflow runners will be launched.\n\n"
                + "This must be compatible with the location of the source and index BigQuery datasets. Note the valid "
                + "locations for [BigQuery](https://cloud.google.com/bigquery/docs/locations) and "
                + "[Dataflow](https://cloud.google.com/dataflow/docs/resources/locations) are not identical. "
                + "In particular, BigQuery has multi-regions (e.g. `US`) and Dataflow does not. If the BigQuery "
                + "datasets are located in a region, the Dataflow location must match. If the BigQuery datasets are "
                + "located in a multi-region, the Dataflow location must be one of the sub-regions (e.g. `US` for "
                + "BigQuery, `us-central1` for Dataflow).")
    public String dataflowLocation;

    @AnnotatedField(
        name = "SZDataflow.gcsTempDirectory",
        markdown =
            "GCS directory where the Dataflow runners will write temporary files.\n\n"
                + "The bucket location must match the [Dataflow location](${SZDataflow.dataflowLocation}). "
                + "This cannot be a path to a top-level bucket, it must contain at least one directory "
                + "(e.g. `gs://mybucket/temp/` not `gs://mybucket`. If this property is unset, Dataflow will attempt to "
                + "create a bucket in the correct location. This may fail if the credentials don't have "
                + "permissions to create buckets. More information in the Dataflow pipeline basic options "
                + "[documentation](https://cloud.google.com/dataflow/docs/reference/pipeline-options#basic_options) "
                + "and other [related documentation](https://cloud.google.com/dataflow/docs/guides/setting-pipeline-options).",
        optional = true)
    public String gcsTempDirectory;

    @AnnotatedField(
        name = "SZDataflow.workerMachineType",
        markdown =
            "Machine type of the Dataflow runners.\n\n"
                + "The available options are [documented](https://cloud.google.com/compute/docs/machine-resource) "
                + "for GCP Compute Engine. If this property is unset, Dataflow will choose a machine type. More information in "
                + "the Dataflow pipeline worker-level options [documentation](https://cloud.google.com/dataflow/docs/reference/pipeline-options#worker-level_options).\n\n"
                + "We have been using the `n1-standard-4` machine type for all underlays so far. "
                + "Given that the machine type Dataflow will choose may not remain the same in the future, recommend setting this property.",
        optional = true)
    public String workerMachineType;

    @AnnotatedField(
        name = "SZDataflow.usePublicIps",
        markdown =
            "Specifies whether the Dataflow runners use external IP addresses.\n\n"
                + "If set to false, make sure that [Private Google Access](https://cloud.google.com/vpc/docs/configure-private-google-access#configuring_access_to_google_services_from_internal_ips) "
                + "is enabled for the VPC sub-network that the Dataflow runners will use. More information "
                + "in the Dataflow pipeline security and networking options [documentation](https://cloud.google.com/dataflow/docs/reference/pipeline-options#security_and_networking). "
                + "We have seen noticeable improvements in speed of running indexing jobs with this set to `false`.",
        optional = true,
        defaultValue = "true")
    public boolean usePublicIps;

    @AnnotatedField(
        name = "SZDataflow.vpcSubnetworkName",
        markdown =
            "Specifies which VPC sub-network the Dataflow runners use.\n\n"
                + "This property is the name of the sub-network (e.g. mysubnetwork), not the full URL path to it "
                + "(e.g. https://www.googleapis.com/compute/v1/projects/my-cloud-project/regions/us-central1/subnetworks/mysubnetwork). "
                + "If this property is unset, Dataflow will try to use a VPC network called \"default\".\n\n"
                + "If you have a custom-mode VPC network, you must set this property. Dataflow can only "
                + "choose the sub-network automatically for auto-mode VPC networks. More information in the "
                + "Dataflow network and subnetwork [documentation](https://cloud.google.com/dataflow/docs/guides/specifying-networks#network_parameter).",
        optional = true,
        defaultValue = "true")
    public String vpcSubnetworkName;
  }
}
