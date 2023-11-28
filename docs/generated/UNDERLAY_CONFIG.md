# Underlay Configuration

This file lists all the configuration properties available for an underlay, including defining the data mapping and the indexing and service deployment data pointers. 
This documentation is generated from annotations in the configuration classes.

* [SZDataflow](#szdataflow)
* [SZIndexer](#szindexer)

## SZDataflow
Properties to pass to Dataflow when kicking off jobs.

### SZDataflow.dataflowLocation
**required** String

Location where the Dataflow runners will be launched.

This must be compatible with the location of the source and index BigQuery datasets. Note the valid locations for [BigQuery](https://cloud.google.com/bigquery/docs/locations) and [Dataflow](https://cloud.google.com/dataflow/docs/resources/locations) are not identical. In particular, BigQuery has multi-regions (e.g. `US`) and Dataflow does not. If the BigQuery datasets are located in a region, the Dataflow location must match. If the BigQuery datasets are located in a multi-region, the Dataflow location must be one of the sub-regions (e.g. `US` for BigQuery, `us-central1` for Dataflow).

### SZDataflow.serviceAccountEmail
**required** String

Email of the service account that the Dataflow runners will use.

The credentials used to kickoff the indexing must have the `iam.serviceAccounts.actAs` permission on this service account. More details in the [GCP documentation](https://cloud.google.com/iam/docs/service-accounts-actas).



## SZIndexer
Define a version of this file for each place you will run indexing. If you later copy the index dataset to other places, you do not need a separate configuration for those.

### SZIndexer.bigQuery
**required** SZBigQuery

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



