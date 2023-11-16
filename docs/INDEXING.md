- [Indexing](#indexing)
  * [Running Indexing Jobs](#running-indexing-jobs)
    + [Setup Credentials](#setup-credentials)
      + [Default Application Credentials](#default-application-credentials)
      + [gcloud Credentials](#gcloud-credentials)
    + [Create Index Dataset](#create-index-dataset)
    + [Kickoff Jobs](#kickoff-jobs)
      + [All Jobs](#all-jobs)
      + [Jobs for One Entity/Group](#jobs-for-one-entitygroup)
  * [Troubleshooting](#troubleshooting)
    + [Concurrency](#concurrency)
    + [Re-Run Jobs](#re-run-jobs)
    + [Run dataflow locally](#run-dataflow-locally)
  * [OMOP Example](#omop-example)


# Indexing
Tanagra can query the source data directly, but **for improved performance, Tanagra generates indexed tables and queries 
them instead**. The indexer config specifies where Tanagra can write generated index tables.

**Generating index tables is part of the deployment process**; It is not managed by the service. There is a basic
command line interface to run the indexing jobs. Currently, this CLI just uses Gradle's application plugin, so the
commands are actually Gradle commands.

## Running Indexing Jobs
Before running the indexing jobs, you need to specify the data mapping and indexer [config files](CONFIG_FILES.md).

There are 3 steps to generating the index tables:
1. [Setup](#setup-credentials) credentials with read permissions on the source data, and read-write permissions on 
the index data.
2. [Validate](#validate-underlay-config) the user-specified underlay config.
3. [Create](#create-index-dataset) the index dataset, if it doesn't already exist.
4. [Kickoff](#kickoff-jobs) the jobs.

Below you can see an [example](#omop-example) of the commands for an OMOP dataset.

### Setup Credentials
You need to set up 2 types of credentials to run all parts of indexing:
1. Default application credentials to allow Tanagra to talk to BigQuery.
2. gcloud credentials to allow you to create a new BigQuery dataset using the `bq` CLI.

#### Default Application Credentials
Set the default application credentials to an account that has read permissions on the source data, and read-write 
permissions on the index data. Best practice is to use a service account that has indexing-specific permissions, but
for debugging, end-user credentials can be very useful.

To use a service account key file, you can set an environment variable:
```
export GOOGLE_APPLICATION_CREDENTIALS=$(PWD)/rendered/tanagra_sa.json
```

To use end-user credentials, you can run:
```
gcloud auth application-default login
```
More information and ways to set up application default credentials in 
[the GCP docs](https://cloud.google.com/docs/authentication/provide-credentials-adc).

#### gcloud Credentials
To use a service account key file:
```
gcloud auth activate-service-account --key-file=$GOOGLE_APPLICATION_CREDENTIALS
```

To use end-user credentials:
```
gcloud auth login
```

### Create Index Dataset
Create a new BigQuery dataset to hold the indexed tables.
Change the location, project, and dataset name below to your own.
```
bq mk --location=location project_id:dataset_id
```

### Kickoff Jobs

#### All Jobs
Do a dry run of all the indexing jobs. This provides a sanity check that the indexing jobs inputs, especially the SQL 
query inputs, are valid. This step is not required, but highly recommended to help catch errors/bugs sooner and without 
running a bunch of computation first. Dry run includes validating that the attribute data types specified match those
returned by running the SQL query. The second argument is the name of an indexer config file.
```
./gradlew indexer:index -Dexec.args="INDEX_ALL cmssynpuf_verily DRY_RUN"
```
Now actually kick off all the indexing jobs.
```
./gradlew indexer:index -Dexec.args="INDEX_ALL cmssynpuf_verily"
```
This can take a long time to complete. If e.g. your computer falls asleep or you need to kill the process on your
computer, you can re-run the same command again. You need to check that there are no in-progress Dataflow jobs in the
project before kicking it off again, because the jobs check for the existence of the output BQ table (not whether there
are any in-progress Dataflow jobs) to tell if they need to run.

#### Jobs for One Entity/Group
You can also kickoff the indexing jobs for a single entity or entity group. This is helpful for testing and debugging.
To kick off all the indexing jobs for a particular entity:
```
./gradlew indexer:index -Dexec.args="INDEX_ENTITY cmssynpuf_verily person DRY_RUN"
./gradlew indexer:index -Dexec.args="INDEX_ENTITY cmssynpuf_verily person"
```
or entity group:
```
./gradlew indexer:index -Dexec.args="INDEX_ENTITY_GROUP cmssynpuf_verily DRY_RUN"
./gradlew indexer:index -Dexec.args="INDEX_ENTITY_GROUP cmssynpuf_verily conditionPerson"
```
All the entities in a group should be indexed before the group. The `INDEX_ALL` command ensures this ordering, but keep 
this in mind if you're running the jobs for each entity or entity group separately.

## Troubleshooting

### Concurrency
By default, the indexing jobs are run concurrently as much as possible. You can force it to run jobs serially by
appending `SERIAL` to the command:
```
./gradlew indexer:index -Dexec.args="INDEX_ALL cmssynpuf_verily DRY_RUN SERIAL"
./gradlew indexer:index -Dexec.args="INDEX_ALL cmssynpuf_verily NOT_DRY_RUN SERIAL"
```

### Re-Run Jobs
Indexing jobs will not overwrite existing index tables. If you want to re-run indexing, either for a single entity/group 
or for everything, you need to delete any existing index tables. You can either do that manually or using the clean
commands below. Similar to the indexing commands, the clean commands also respect the dry run flag.

To clean the generated index tables for everything:
```
./gradlew indexer:index -Dexec.args="CLEAN_ALL cmssynpuf_verily DRY_RUN"
./gradlew indexer:index -Dexec.args="CLEAN_ALL cmssynpuf_verily"
```
or a particular entity:
```
./gradlew indexer:index -Dexec.args="CLEAN_ENTITY cmssynpuf_verily person DRY_RUN"
./gradlew indexer:index -Dexec.args="CLEAN_ENTITY cmssynpuf_verily person"
```
or a particular entity group:
```
./gradlew indexer:index -Dexec.args="CLEAN_ENTITY_GROUP cmssynpuf_verily person DRY_RUN"
./gradlew indexer:index -Dexec.args="CLEAN_ENTITY_GROUP cmssynpuf_verily person"
```

### Run dataflow locally
While developing a job, running locally is faster. Also, you can use Intellij debugger.
- Add to `BigQueryIndexingJob.buildDataflowPipelineOptions()`:
  ```
  import org.apache.beam.runners.direct.DirectRunner;
  
  dataflowOptions.setRunner(DirectRunner.class);
  dataflowOptions.setTempLocation("gs://dataflow-staging-us-central1-694046000181/temp");
  ```
- Filter your queries on one person, eg:
  ```
  .where(
      new BinaryFilterVariable(
          idFieldVar,
          BinaryOperator.EQUALS,
          new Literal.Builder().dataType(DataType.INT64).int64Val(1107050).build()))
  ```

## OMOP Example
The `cmssynpuf` underlay is a data mapping for a [public dataset](https://console.cloud.google.com/marketplace/product/hhs/synpuf) 
that uses the OMOP schema.

You can see the top-level underlay config file for this dataset [here](../underlay/src/main/resources/config/underlay/cmssynpuf/underlay.json).

```
bq mk --location=US verily-tanagra-dev:cmssynpuf_index

./gradlew indexer:index -Dexec.args="INDEX_ALL cmssynpuf_verily DRY_RUN"
./gradlew indexer:index -Dexec.args="INDEX_ALL cmssynpuf_verily"
```
