package bio.terra.tanagra.underlay.serialization;

/**
 * <p>Indexer configuration.</p>
 * <p>Define a version of this file for each place you will run indexing.
 * If you later copy the index dataset to other places, you do not need a separate configuration for those.</p>
 */
public class SZIndexer {
  /**
   * <p>Name of the underlay to index.</p>
   * <p>Name is specified in the underlay file, and also matches the name of the config/underlay sub-directory in the underlay sub-project resources (e.g. cmssynpuf).</p>
   */
  public String underlay;

  /**
   * <p>Pointers to the source and index BigQuery datasets.</p>
   */
  public SZBigQuery bigQuery;

  /**
   * <p>Dataflow configuration.</p>
   * <p>Required for indexing jobs that use batch processing (e.g. computing the ancestor-descendant pairs for a hierarchy).</p>
   */
  public Dataflow dataflow;

  /**
   * <p>Dataflow configuration properties.</p>
   */
  public static class Dataflow {
    /**
     * <p>Email of the service account that the Dataflow runners will use.</p>
     * <p>The credentials used to kickoff the indexing must have the <code>iam.serviceAccounts.actAs</code> permission on this service account.
     * See <a href="https://cloud.google.com/iam/docs/service-accounts-actas">GCP documentation</a> for more information on this permission.</p>
     */
    public String serviceAccountEmail;

    /**
     * <p>Location where the Dataflow runners will be launched.</p>
     * <p>This must be compatible with the location of the source and index BigQuery datasets.
     * Note the valid locations for <a href="https://cloud.google.com/bigquery/docs/locations">BigQuery</a> and <a href="https://cloud.google.com/dataflow/docs/resources/locations">Dataflow</a> are not identical.
     * In particular, BigQuery has multi-regions (e.g. <code>US</code>) and Dataflow does not.
     * If the BigQuery datasets are located in a region, the Dataflow location must match.
     * If the BigQuery datasets are located in a multi-region, the Dataflow location must be one of the sub-regions (e.g. <code>US</code> for BigQuery, <code>us-central1</code> for Dataflow).</p>
     */
    public String dataflowLocation;

    /**
     * <p><strong>(optional)</strong> GCS directory where the Dataflow runners will write temporary files.</p>
     * <p>The bucket location must match the Dataflow location above.
     * This cannot be a path to a top-level bucket, it must contain at least one directory (e.g. <code>gs://mybucket/temp/</code> not <code>gs://mybucket/</code>.
     * If this property is unset, Dataflow will attempt to create a bucket in the correct location. This may fail if the credentials don't have permissions to create buckets.
     * More information in the Dataflow pipeline basic options <a href="https://cloud.google.com/dataflow/docs/reference/pipeline-options#basic_options">documentation</a>
     * and other <a href="https://cloud.google.com/dataflow/docs/guides/setting-pipeline-options">related documentation</a>.</p>
     */
    public String gcsTempDirectory;

    /**
     * <p><strong>(optional)</strong> Machine type of the Dataflow runners.</p>
     * <p>The available options are <a href="https://cloud.google.com/compute/docs/machine-resource">documented</a> for GCP Compute Engine.
     * If this property is unset, Dataflow will choose a machine type.
     * More information in the Dataflow pipeline worker-level options <a href="https://cloud.google.com/dataflow/docs/reference/pipeline-options#worker-level_options">documentation</a>.</p>
     * <p>We have been using the <code>n1-standard-4</code> machine type for all underlays so far.
     * Given that the machine type Dataflow will choose may not remain the same in the future, we recommend setting this property.</p>
     */
    public String workerMachineType;

    /**
     * <p><strong>(optional)</strong> Specifies whether the Dataflow runners use external IP addresses.</p>
     * <p>Default is <code>true</code>.
     * If set to false, make sure that <a href="https://cloud.google.com/vpc/docs/configure-private-google-access#configuring_access_to_google_services_from_internal_ips">Private Google Access</a>
     * is enabled for the VPC sub-network that the Dataflow runners will use.
     * More information in the Dataflow pipeline security and networking options <a href="https://cloud.google.com/dataflow/docs/reference/pipeline-options#security_and_networking">documentation</a>.</p>
     * <p>We have seen noticeable improvements in speed of running indexing jobs with this set to <code>false</code>.</p>
     */
    public boolean usePublicIps;

    /**
     * <p><strong>(optional)</strong> Specifies which VPC sub-network the Dataflow runners use.</p>
     * <p>This property is the name of the sub-network (e.g. mysubnetwork), not the full URL path to it
     * (e.g. https://www.googleapis.com/compute/v1/projects/my-cloud-project/regions/us-central1/subnetworks/mysubnetwork).
     * If this property is unset, Dataflow will try to use a VPC network called "default".
     * </p>
     * <p>If you have a custom-mode VPC network, you must set this property.
     * Dataflow can only choose the sub-network automatically for auto-mode VPC networks.
     * More information in the Dataflow network and subnetwork <a href="https://cloud.google.com/dataflow/docs/guides/specifying-networks#network_parameter">documentation</a>.</p>
     */
    public String vpcSubnetworkName;
  }
}
