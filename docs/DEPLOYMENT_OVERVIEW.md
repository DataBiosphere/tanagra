# Deployment Overview

Tanagra has two sets of GCP requirements, for the indexer environment and for the service deployment. Indexer 
environments and service deployments are not one-to-one. You can have multiple indexer environments for a single 
service deployment and vice versa.

Once you have an environment or deployment setup, you need to set the relevant properties in the config files (e.g. GCP
project id, BigQuery dataset id, GCS bucket name). Pointers to the relevant config properties are included in each
section below.

## Indexer Environment
An indexer environment is a GCP project configured with the items below.

- These APIs [enabled](https://cloud.google.com/endpoints/docs/openapi/enable-api).
Some of these may be enabled by default in your project.
   - [BigQuery](https://cloud.google.com/bigquery) `bigquery.googleapis.com`
   - [Cloud Storage](https://cloud.google.com/storage) `storage.googleapis.com`
   - [Dataflow](https://cloud.google.com/dataflow) `dataflow.googleapis.com`
- GCS bucket in the same location as the source and index BigQuery datasets.
- **"VM"** service account with the below permissions to attach to the Dataflow worker VMs.
   - Read the source BigQuery dataset. `roles/bigquery.dataViewer` granted at the dataset-level (on the source dataset)
     includes the [required permissions](https://cloud.google.com/bigquery/docs/access-control#bigquery.dataViewer).
   - Create BigQuery jobs. `roles/bigquery.jobUser` granted at the project-level (on the indexer GCP project)
     includes the [required permissions](https://cloud.google.com/bigquery/docs/access-control#bigquery.jobUser).
   - Write to the index BigQuery dataset. `roles/bigquery.dataOwner` granted at the dataset-level (on the index dataset)
   includes the [required permissions](https://cloud.google.com/bigquery/docs/access-control#bigquery.dataOwner).
   - Execute Dataflow work units. `roles/dataflow.worker` granted at the project-level (on the indexer GCP project)
     includes the [required permissions](https://cloud.google.com/dataflow/docs/concepts/access-control#dataflow.worker).
- **"Runner"** end-user or service account with the below permissions to run indexing.
   - Read the source BigQuery dataset. `roles/bigquery.dataViewer` granted at the dataset-level (on the source dataset)
     includes the [required permissions](https://cloud.google.com/bigquery/docs/access-control#bigquery.dataViewer).
   - Create BigQuery jobs. `roles/bigquery.jobUser` granted at the project-level (on the indexer GCP project)
   includes the [required permissions](https://cloud.google.com/bigquery/docs/access-control#bigquery.jobUser).
   - Create/delete and write to the index BigQuery dataset. `roles/bigquery.dataOwner` granted at the project-level (on
     the indexer GCP project) includes the [required permissions](https://cloud.google.com/bigquery/docs/access-control#bigquery.dataOwner).
   - Kickoff Dataflow jobs. `roles/dataflow.admin` granted at the project-level (on the indexer GCP project)
   includes the [required permissions](https://cloud.google.com/dataflow/docs/concepts/access-control#dataflow.admin).
   - Attach the **"VM"** service account credentials to the Dataflow worker VMs. `roles/iam.serviceAccountUser` granted 
   at the service account-level (on the "VM" service account) 
   includes the [required permissions](https://cloud.google.com/compute/docs/access/iam#the_serviceaccountuser_role).
- (Optional) VPC sub-network configured with [Private Google Access](https://cloud.google.com/vpc/docs/configure-private-google-access#configuring_access_to_google_services_from_internal_ips)
  to help speed up the Dataflow jobs.

You can use a single service account for both the "VM" and "runner" use cases, as long as it has all the permissions.

### Config properties
All indexer configuration lives in the indexer config file, the properties of which are all documented 
[here](./generated/UNDERLAY_CONFIG.md#szindexer). In particular:

  - Set the pointer to the [index BigQuery dataset](./generated/UNDERLAY_CONFIG.md#szindexerbigquery) that you create 
with the **"runner"** credentials above.
  - Set all the [Dataflow](./generated/UNDERLAY_CONFIG.md#szindexerdataflow) properties. 
    - A directory in the GCS bucket above as the Dataflow [temp location](./generated/UNDERLAY_CONFIG.md#szdataflowgcstempdirectory).
    - The **"VM"** service account as the Dataflow [service account email](./generated/UNDERLAY_CONFIG.md#szdataflowserviceaccountemail).
    - (Optional) The Dataflow [VPC sub-network](./generated/UNDERLAY_CONFIG.md#szdataflowvpcsubnetworkname) if you have 
    a custom-mode network.
    - (Optional) The Dataflow [use public IPs](./generated/UNDERLAY_CONFIG.md#szdataflowusepublicips) flag if you want 
    to use Private Google Access.

## Service Deployment
A service deployment lives in a GCP project configured with the items below.

- Java service packaged as a runnable JAR file, deployed either in 
    [GKE](https://cloud.google.com/kubernetes-engine/docs/quickstarts/deploy-app-container-image#deploying_to_gke) or 
    [AppEngine](https://cloud.google.com/eclipse/docs/deploy-flex-jar#deploy_a_jar_or_war_file).
- CloudSQL database, either [PostGres](https://cloud.google.com/sql/docs/postgres) (recommended) or 
    [MySQL](https://cloud.google.com/sql/docs/mysql).
- GCS bucket for export files, one per index dataset location.
    - [Update the CORS configuration](https://cloud.google.com/storage/docs/using-cors#command-line) with any URLs that 
   will need to read exported files. e.g. If there is an export model that writes a file and redirects to another URL 
   that will read the file, you will likely need to grant that URL permission to make `GET` requests for objects in the 
   bucket. Example CORS configuration file:
   ```
   [
    {
      "origin": ["https://workbench.verily.com"],
      "method": ["GET"],
      "responseHeader": ["Content-Type"],
      "maxAgeSeconds": 3600
    }
   ]
   ```
- **"Application"** service account with the below permissions.
    - Read the source BigQuery dataset. `roles/bigquery.dataViewer` granted at the dataset-level (on the source dataset)
      includes the [required permissions](https://cloud.google.com/bigquery/docs/access-control#bigquery.dataViewer).
    - Read the index BigQuery dataset. `roles/bigquery.dataViewer` granted at the dataset-level (on the index dataset)
      includes the [required permissions](https://cloud.google.com/bigquery/docs/access-control#bigquery.dataViewer).
    - Create BigQuery jobs. `roles/bigquery.jobUser` granted at the project-level (on the service GCP project)
      includes the [required permissions](https://cloud.google.com/bigquery/docs/access-control#bigquery.jobUser).
    - Read and write files to the export bucket(s). `roles/storage.objectAdmin` granted at the bucket-level includes the
      [required permissions](https://cloud.google.com/storage/docs/access-control/iam-roles#standard-roles).
    - Generate signed URLs for export files. `roles/iam.serviceAccountTokenCreator` granted at the service account-level
      (on itself) includes the [required permissions](https://cloud.google.com/iam/docs/service-account-permissions#impersonate).
    - Talk to the CloudSQL database. `roles/cloudsql.client` granted at the project-level includes the 
      [required permissions](https://cloud.google.com/sql/docs/mysql/iam-roles#roles).

### Config properties
Service configuration lives in two places, depending on whether they apply to a single underlay or the entire
deployment.

#### Single underlay
Each underlay hosted by a service deployment has its own service config file.
All service config file properties are documented [here](./generated/UNDERLAY_CONFIG.md#szservice). In particular:

- Set the pointer to the [index BigQuery dataset](./generated/UNDERLAY_CONFIG.md#szservicebigquery) that you create
  with the indexer **"runner"** credentials above.

#### Entire deployment
Each service deployment is a single Java application. You can configure this Java application with a custom 
`application.yaml` file, or (more common) override the [default application properties](../service/src/main/resources/application.yml) 
with environment variables. All application properties are documented [here](./generated/APPLICATION_CONFIG.md).
In particular:

- Set the GCS bucket(s) above as the [shared export](./generated/APPLICATION_CONFIG.md#export-shared) buckets.
- Set the [application DB properties](./generated/APPLICATION_CONFIG.md#application-database) with the CloudSQL 
information.

## Deployment Patterns

### Single project for indexer and service
This is probably the default for getting up and running with Tanagra. You can use the same GCP project for both the
indexer environment and the service deployment. It may also be useful for automated testing or dev environments.
For production services though, we recommend separating the indexer and service projects.

### Indexer environment supports multiple service deployments
A single indexer environment can support multiple service deployments. There is one GCP project that is configured
correctly for indexing (i.e. Dataflow is enabled, there are one or more service accounts with permissions to write
to BigQuery and kickoff Dataflow jobs, etc.). Multiple underlays or multiple versions of a single underlay are
indexed in this project, each into its own index BigQuery dataset. You could use different service accounts for each 
source dataset.

Service deployments may then read/query these index datasets directly from the indexer environment project. Or you can
"publish" (i.e. copy) the index datasets to another project (e.g. the service deployment project). The Verily `test` 
and `dev` service deployments both read directly from the indexer environment project. The AoU production service 
deployment will add the "publish" step.

Separating the indexer environment from the service deployment means you can avoid increasing permissions in the
service deployment project (e.g. don't need to enable Dataflow in your audited production project).

### Service deployment hosts multiple underlays
A single service deployment can host one or more underlays. Each underlay should have its own service configuration
file. The service deployment configuration [allows specifying](./generated/APPLICATION_CONFIG.md#underlays) multiple
service configuration files.

Keep in mind that the access control implementation is per deployment, not per underlay. So if you want
underlay-specific access control, then you should modify your access control implementation to have different
behavior depending on which underlay is being used.

The Verily `test` and `dev` service deployments both host multiple underlays.
