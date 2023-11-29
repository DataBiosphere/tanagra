# Underlay Configuration

This file lists all the configuration properties available for an underlay, including defining the data mapping and the indexing and service deployment data pointers. 
This documentation is generated from annotations in the configuration classes.

* [SZBigQuery](#szbigquery)
* [SZDataflow](#szdataflow)
* [SZIndexer](#szindexer)
* [SZMetadata](#szmetadata)
* [SZService](#szservice)
* [SZUnderlay](#szunderlay)

## SZBigQuery
Pointers to the source and index BigQuery datasets.

### SZBigQuery.dataLocation
**required** String

Valid locations for BigQuery are listed in the GCP [documentation](https://cloud.google.com/bigquery/docs/locations).

### SZBigQuery.indexData
**required** IndexData

Pointer to the index BigQuery dataset.

### SZBigQuery.queryProjectId
**required** String

Queries will be run in this project.

This is the project that will be billed for running queries. For the indexer, this project is also where the Dataflow jobs will be kicked off. Often this project will be the same project as the one where the index and/or source datasets live.

However, sometimes it will be different. For example, the source dataset may be a public dataset that we don't have billing access to. In that case, the indexer configuration must specify a different query project id. As another example, the source and index datasets may live in a project that is shared across service deployments. In that case, the service configurations may specify a different query project id for each deployment.

### SZBigQuery.sourceData
**required** SourceData

Pointer to the source BigQuery dataset.



## SZDataflow
Properties to pass to Dataflow when kicking off jobs.

### SZDataflow.dataflowLocation
**required** String

Location where the Dataflow runners will be launched.

This must be compatible with the location of the source and index BigQuery datasets. Note the valid locations for [BigQuery](https://cloud.google.com/bigquery/docs/locations) and [Dataflow](https://cloud.google.com/dataflow/docs/resources/locations) are not identical. In particular, BigQuery has multi-regions (e.g. `US`) and Dataflow does not. If the BigQuery datasets are located in a region, the Dataflow location must match. If the BigQuery datasets are located in a multi-region, the Dataflow location must be one of the sub-regions (e.g. `US` for BigQuery, `us-central1` for Dataflow).

### SZDataflow.gcsTempDirectory
**optional** String

GCS directory where the Dataflow runners will write temporary files.

The bucket location must match the [Dataflow location](#szdataflowdataflowlocation). This cannot be a path to a top-level bucket, it must contain at least one directory (e.g. `gs://mybucket/temp/` not `gs://mybucket`. If this property is unset, Dataflow will attempt to create a bucket in the correct location. This may fail if the credentials don't have permissions to create buckets. More information in the Dataflow pipeline basic options [documentation](https://cloud.google.com/dataflow/docs/reference/pipeline-options#basic_options) and other [related documentation](https://cloud.google.com/dataflow/docs/guides/setting-pipeline-options).

### SZDataflow.serviceAccountEmail
**required** String

Email of the service account that the Dataflow runners will use.

The credentials used to kickoff the indexing must have the `iam.serviceAccounts.actAs` permission on this service account. More details in the [GCP documentation](https://cloud.google.com/iam/docs/service-accounts-actas).

### SZDataflow.usePublicIps
**optional** boolean

Specifies whether the Dataflow runners use external IP addresses.

If set to false, make sure that [Private Google Access](https://cloud.google.com/vpc/docs/configure-private-google-access#configuring_access_to_google_services_from_internal_ips) is enabled for the VPC sub-network that the Dataflow runners will use. More information in the Dataflow pipeline security and networking options [documentation](https://cloud.google.com/dataflow/docs/reference/pipeline-options#security_and_networking). We have seen noticeable improvements in speed of running indexing jobs with this set to `false`.

*Default value:* `true`

### SZDataflow.vpcSubnetworkName
**optional** String

Specifies which VPC sub-network the Dataflow runners use.

This property is the name of the sub-network (e.g. mysubnetwork), not the full URL path to it (e.g. https://www.googleapis.com/compute/v1/projects/my-cloud-project/regions/us-central1/subnetworks/mysubnetwork). If this property is unset, Dataflow will try to use a VPC network called "default".

If you have a custom-mode VPC network, you must set this property. Dataflow can only choose the sub-network automatically for auto-mode VPC networks. More information in the Dataflow network and subnetwork [documentation](https://cloud.google.com/dataflow/docs/guides/specifying-networks#network_parameter).

*Default value:* `true`

### SZDataflow.workerMachineType
**optional** String

Machine type of the Dataflow runners.

The available options are [documented](https://cloud.google.com/compute/docs/machine-resource) for GCP Compute Engine. If this property is unset, Dataflow will choose a machine type. More information in the Dataflow pipeline worker-level options [documentation](https://cloud.google.com/dataflow/docs/reference/pipeline-options#worker-level_options).

We have been using the `n1-standard-4` machine type for all underlays so far. Given that the machine type Dataflow will choose may not remain the same in the future, recommend setting this property.



## SZIndexer
Indexer configuration.

Define a version of this file for each place you will run indexing. If you later copy the index dataset to other places, you do not need a separate configuration for those.

### SZIndexer.bigQuery
**required** [SZBigQuery](#szbigquery)

Pointers to the source and index BigQuery datasets.

### SZIndexer.dataflow
**required** [SZDataflow](#szdataflow)

Dataflow configuration.

Required for indexing jobs that use batch processing (e.g. computing the ancestor-descendant pairs for a hierarchy).

### SZIndexer.underlay
**required** String

Name of the underlay to index.

Name is specified in the underlay file, and also matches the name of the config/underlay sub-directory in the underlay sub-project resources.

*Example value:* `cmssynpuf`



## SZMetadata
Metadata for the underlay.

Information in this object is not used in the operation of the indexer or service, it is for display purposes only.

### SZMetadata.description
**optional** String

Description of the underlay.

### SZMetadata.displayName
**required** String

Display name for the underlay.

Unlike the underlay [name](#szunderlayname), it may include spaces and special characters.

### SZMetadata.properties
**optional** Map [ String, String ]

Key-value map of underlay properties.

Keys may not include spaces or special characters, only letters and numbers.



## SZService
Service configuration.

Define a version of this file for each place you will deploy the service. If you share the same index dataset across multiple service deployments, you need a separate configuration for each.

### SZService.bigQuery
**required** [SZBigQuery](#szbigquery)

Pointers to the source and index BigQuery datasets.

### SZService.underlay
**required** String

Name of the underlay to make available in the service deployment.

If a single deployment serves multiple underlays, you need a separate configuration for each. Name is specified in the underlay file, and also matches the name of the config/underlay sub-directory in the underlay sub-project resources.

*Example value:* `cmssynpuf`



## SZUnderlay
Underlay configuration.

Define a version of this file for each dataset. If you index and/or serve a dataset in multiple places or deployments, you only need one version of this file.

### SZUnderlay.criteriaOccurrenceEntityGroups
**required** Set [ String ]

List of paths of `criteria-occurrence` type entity groups.

A `criteria-occurrence` type entity group defines a relationship between three entities.

Path consists of two parts: [Data-Mapping Group]/[Entity Group Name] (e.g. `omop/conditionPerson`).

[Data-Mapping Group] is the name of a sub-directory of the config/datamapping/ sub-directory in the underlay sub-project resources (e.g. `omop`).

[Entity Group Name] is specified in the entity group file, and also matches the name of the sub-directory of the config/datamapping/[Data-Mapping Group]/entitygroup sub-directory in the underlay sub-project resources (e.g. `conditionPerson`).

Using the path here instead of just the entity group name allows us to share entity group definitions across underlays. For example, the `omop` data-mapping group contains template entity group definitions for standing up a new underlay.

### SZUnderlay.entities
**required** Set [ String ]

List of paths of all the entities.

An entity is any object that the UI might show a list of (e.g. list of persons, conditions, condition occurrences). The list must include the primary entity.

Path consists of two parts: [Data-Mapping Group]/[Entity Name] (e.g. `omop/condition`).

[Data-Mapping Group] is the name of a sub-directory of the config/datamapping/ sub-directory in the underlay sub-project resources (e.g. `omop`).

[Entity Name] is specified in the entity file, and also matches the name of the sub-directory of the config/datamapping/[Data-Mapping Group]/entity sub-directory in the underlay sub-project resources (e.g. `condition`).

Using the path here instead of just the entity name allows us to share entity definitions across underlays. For example, the `omop` data-mapping group contains template entity definitions for standing up a new underlay.

### SZUnderlay.groupItemsEntityGroups
**required** Set [ String ]

List of paths of `group-items` type entity groups.

A `group-items` type entity group defines a relationship between two entities.

Path consists of two parts: [Data-Mapping Group]/[Entity Group Name] (e.g. `omop/brandIngredient`).

[Data-Mapping Group] is the name of a sub-directory of the config/datamapping/ sub-directory in the underlay sub-project resources (e.g. `omop`).

[Entity Group Name] is specified in the entity group file, and also matches the name of the sub-directory of the config/datamapping/[Data-Mapping Group]/entitygroup sub-directory in the underlay sub-project resources (e.g. `brandIngredient`).

Using the path here instead of just the entity group name allows us to share entity group definitions across underlays. For example, the `omop` data-mapping group contains template entity group definitions for standing up a new underlay.

### SZUnderlay.metadata
**required** [SZMetadata](#szmetadata)

Metadata for the underlay.

### SZUnderlay.name
**required** String

Name of the underlay.

This is the unique identifier for the underlay. If you serve multiple underlays in a single service deployment, the underlay names cannot overlap. Name may not include spaces or special characters, only letters and numbers.

This name is stored in the application database for cohorts and data feature sets, so once there are artifacts associated with an underlay, you can't change the underlay name.

### SZUnderlay.primaryEntity
**required** String

Name of the primary entity.

A cohort contains instances of the primary entity (e.g. persons).

### SZUnderlay.uiConfigFile
**required** String

Name of the UI config file.

File must be in the same directory as the underlay file. Name includes file extension.

*Example value:* `ui.json`



